package com.atguigu.gmall.order.service;

import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/20 14:56
 */
public interface OrderService {
    OrderConfirmVo confirm();

    void submit(OrderSubmitVo submitVo);
}
