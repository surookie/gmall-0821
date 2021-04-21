package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/20 11:07
 */
@Data
public class OrderConfirmVo {
    // 收件人地址列表
    private List<UserAddressEntity> addresses;

    // 订单、送货详情
    private List<OrderItemVo> orderItems;

    // 购物积分
    private Integer bounds;

    private String orderToken; // 为了防止重复提交
}