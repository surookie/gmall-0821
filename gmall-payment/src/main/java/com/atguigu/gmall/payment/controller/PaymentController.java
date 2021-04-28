package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entiry.PaymentInfoEntity;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * @Description 支付
 * @Author rookie
 * @Date 2021/4/28 12:07
 */
@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken") String orderToken, Model model) {
        OrderEntity orderEntity = this.orderJudge(orderToken);
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody
    public String alipay(@RequestParam("orderToken") String orderToken) throws AlipayApiException {
        OrderEntity orderEntity = this.orderJudge(orderToken);
        // 调用阿里的支付接口，跳转到支付页面
        PayVo payVo = new PayVo();
        // 设置订单编号
        payVo.setOut_trade_no(orderToken);
        payVo.setSubject("guli store system");
        // 设置订单价格   payVo.setTotal_amount(orderEntity.getTotalAmount().toString());
        // test
        payVo.setTotal_amount("0.01");

        // 生成对账记录，将对账记录的id传给支付宝，让其在传给后续的业务接口方法
        String payId = this.paymentService.savePayment(orderEntity);
        payVo.setPassback_params(payId);
        String form = alipayTemplate.pay(payVo);
        return form;
    }

    @GetMapping("pay/success")
    public String toPaySuccess() {

        return "paysuccess";
    }

    @PostMapping("pay/ok")
    @ResponseBody
    public Object payOk(PayAsyncVo asyncVo) {
        // 1. 验签
        Boolean flag = alipayTemplate.checkSignature(asyncVo);
        if(!flag) {
            return "failure";
        }
        // 2. 检验业务数据
        String payId = asyncVo.getPassback_params();
        String app_id = asyncVo.getApp_id();
        String total_amount = asyncVo.getTotal_amount();
        String out_trade_no = asyncVo.getOut_trade_no();

        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPaymentById(payId);
        if(!StringUtils.equals(app_id, this.alipayTemplate.getApp_id())
                || new BigDecimal(total_amount).compareTo(paymentInfoEntity.getTotalAmount()) != 0
                || !StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo())
        ) {
            return "failure";
        }

        // 3. 检验支付数据
        String trade_status = asyncVo.getTrade_status();
        if (!StringUtils.equals("TRADE_SUCCESS", trade_status)) {
            return "failure";
        }
        // 4. 更新支付对账表
        if (this.paymentService.updatePaymentInfo(asyncVo, payId) == 0) {
            return "failure";
        }
        // 5. 发送给给订单（oms 发送消息给wms减库存）
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.success", out_trade_no);

        // 6. 响应信息给支付宝

        return "success";
    }



    @GetMapping("pay/test")
    @ResponseBody
    public ResponseVo test() {
        return ResponseVo.ok();
    }

    private OrderEntity orderJudge(@RequestParam("orderToken") String orderToken) {
        OrderEntity orderEntity = this.paymentService.queryOrderByOrderToken(orderToken);
        if(orderEntity == null) {
            throw new OrderException("要付的订单不存在");
        }

        // 判断订单是否为该用户的
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        if(userId != orderEntity.getUserId()) {
            throw new OrderException("该订单并非您的，或者您没有支付权限");
        }
        // 判断订单订单状态是否为待付款
        if(orderEntity.getStatus() != 0) {
            throw new OrderException("该订单无法支付，请注意你的订单状态");
        }
        return orderEntity;
    }
}