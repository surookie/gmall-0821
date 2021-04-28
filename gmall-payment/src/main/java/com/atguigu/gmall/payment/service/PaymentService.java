package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entiry.PaymentInfoEntity;
import com.atguigu.gmall.payment.vo.PayAsyncVo;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/28 12:10
 */
public interface PaymentService {
    OrderEntity queryOrderByOrderToken(String orderToken);

    String savePayment(OrderEntity orderEntity);

    PaymentInfoEntity queryPaymentById(String payId);

    int updatePaymentInfo(PayAsyncVo asyncVo, String payId);
}
