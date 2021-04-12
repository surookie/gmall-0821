package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //检索关键字
    private String keyWord;

    //品牌的过滤条件
    private List<Long> brandId;

    //分类的过滤的条件
    private List<Long> categoryId;

    //规格参数的过滤["4:8G","5:8G-16G"]
    private List<String> props;

    //排序字段 0默认，根据得分降序。1-价格降序，2-价格升序，3-销量的降序，4-销量的降序序
    private Integer sort = 0;

    //价格区间过滤
    private Double priceFrom;
    private Double priceTo;
    //是否有货的过滤
    private Boolean store;

    //页码
    private Integer pageNum = 1;
    //分页尺寸
    private final Integer pageSize = 20;
}
