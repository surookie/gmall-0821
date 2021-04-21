package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/20 17:08
 */
@Data
public class OrderSubmitVo {

    private String orderToken; // 防重

    private UserAddressEntity address;

    private Integer payType;

    private String deliveryCompany; //送货公司

    private Integer bounds; // 购物积分

    private List<OrderItemVo> items; // 订单详情

    private BigDecimal totalPrice; // 验总价
}