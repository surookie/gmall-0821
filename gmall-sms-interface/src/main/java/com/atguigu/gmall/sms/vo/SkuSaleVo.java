package com.atguigu.gmall.sms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuSaleVo {
    private Long skuId;
    //sms积分属性
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;
    //满减优惠属性
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
    //打折优惠属性
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;
}
