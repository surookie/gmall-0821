package com.atguigu.gmall.gateway.controller;

import com.atguigu.gmall.gateway.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Description 支付
 * @Author rookie
 * @Date 2021/4/28 12:07
 */
@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken") String orderToken) {

        return "pay";
    }
}