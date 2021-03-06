package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * @Description 订单
 * @Author rookie
 * @Date 2021/4/20 11:03
 */
@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public String confirm(Model model) {
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<String> submit(@RequestBody OrderSubmitVo submitVo) {
        this.orderService.submit(submitVo);
        return ResponseVo.ok(submitVo.getOrderToken());
    }
}