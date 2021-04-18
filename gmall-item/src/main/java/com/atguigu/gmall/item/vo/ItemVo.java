package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Description 商品详情页的自定义封装对象
 * @Author rookie
 * @Date 2021/4/15 8:43
 */
@Data
public class ItemVo {
    // 分类对象
    private List<CategoryEntity> categories;
    // 面包屑：品牌信息
    private Long brandId;
    private String brandName;

    // 面包屑 ：spu信息
    private Long spuId;
    private String spuName;

    // sku信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    // 图片列表
    private List<SkuImagesEntity> images;

    // 营销信息
    private List<ItemSaleVo> sales;

    // 库存信息
    private Boolean store = false;

    // [{attrId:4,attrName:'颜色',attrValues:['暗夜黑','水晶白']}
    // ,{attrId:5,attrName:'内存',attrValues:['8G','16G']}
    // ,{attrId:6,attrName:'储存',attrValues:['128G','256G']}]
    // 当前sku相同的spu下所有sku的销售属性列表
    private List<SaleAttrValueVo> saleAttrs;

    // {4:'暗夜黑',5:'8G',6:'128G'} 高亮
    // 当前sku的销售参数
    private Map<Long, String> saleAttr;

    // 销售属性组合与skuId的映射关系
    private String skuJsons;

    // 商品描述信息
    private List<String> spuImages;

    // 规格参数
    private List<ItemGroupVo> groups;

}