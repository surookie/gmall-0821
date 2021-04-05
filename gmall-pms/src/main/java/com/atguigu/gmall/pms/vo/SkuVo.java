package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuVo extends SkuEntity {

    //sku_image属性
    private List<String> images;
    //销售属性
    private List<SkuAttrValueEntity> saleAttrs;

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
