package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Description 规格参数渲染
 * @Author rookie
 * @Date 2021/4/8 19:14
 */
@Data
public class SearchResponseAttrVo {

    //类型id
    private Long attrId;
    //参数名
    private String attrName;
    //参数值
    private List<String> attrValues;
}