package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entiry.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/28 12:11
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Override
    public OrderEntity queryOrderByOrderToken(String orderToken) {
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByToken(orderToken);
        OrderEntity orderEntity = orderEntityResponseVo.getData();
        return orderEntity;
    }

    @Override
    public String savePayment(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        // 0 - 未支付
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setTotalAmount(new BigDecimal("0.01"));
        paymentInfoEntity.setPaymentType(orderEntity.getPayType());
        paymentInfoEntity.setSubject("guli store system");
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        paymentInfoEntity.setCreateTime(new Date());

        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId().toString();
    }

    @Override
    public PaymentInfoEntity queryPaymentById(String payId) {
        return this.paymentInfoMapper.selectById(payId);
    }

    @Override
    public int updatePaymentInfo(PayAsyncVo asyncVo, String payId) {
        PaymentInfoEntity paymentInfoEntity = this.paymentInfoMapper.selectById(payId);
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setTradeNo(asyncVo.getTrade_no());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(asyncVo));

        int i = this.paymentInfoMapper.updateById(paymentInfoEntity);
        return i;
    }
}