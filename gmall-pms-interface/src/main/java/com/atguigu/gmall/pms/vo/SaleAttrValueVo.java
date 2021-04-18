package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @Description sku的属性封装
 * @Author rookie
 * @Date 2021/4/15 8:58
 */
@Data
public class SaleAttrValueVo {
    private Long attrId;
    private String attrName;
    private Set<String> attrValues;
}