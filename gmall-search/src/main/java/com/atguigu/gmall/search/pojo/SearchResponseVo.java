package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description 搜索条件页面渲染
 * @Author rookie
 * @Date 2021/4/8 19:11
 */
@Data
public class SearchResponseVo {
    // 品牌列表的渲染
    private List<BrandEntity> brands;

    // 分类列表的渲染
    private List<CategoryEntity> categories;

    // 规格参数列表的渲染
    private List<SearchResponseAttrVo> filters;

    //分页所需数据
    private Integer pageNum;
    private Integer pageSize;

    //总记录数
    private Long total;

    //当前页的数据
    private List<Goods> goodsList;
}