package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description 规格参数信息
 * @Author rookie
 * @Date 2021/4/15 9:14
 */
@Data
public class ItemGroupVo {
    private Long id;
    private String name;
    private List<AttrValueVo> attrValue;
}