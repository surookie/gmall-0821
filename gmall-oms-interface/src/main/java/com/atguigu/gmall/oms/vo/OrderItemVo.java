package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/20 11:11
 */
@Data
public class OrderItemVo {
    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性：List<SkuAttrValueEntity>的json格式
    private BigDecimal price; // 加入购物车时的价格
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales; // 营销信息: List<ItemSaleVo>的json格式
    private Integer weight; // 商品重量
}