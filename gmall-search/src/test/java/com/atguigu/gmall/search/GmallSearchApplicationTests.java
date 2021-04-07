package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GmallWmsApi gmallWmsApi;

    @Autowired
    private GmallPmsApi gmallPmsApi;

    private Integer pageNum = 1;
    private Integer pageSize = 100;

    @Test
    void contextLoads() {
        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);
        do {
            PageParamVo pageParamVo = new PageParamVo(pageNum, pageSize, null);
            ResponseVo<List<SpuEntity>> listResponseVo = this.gmallPmsApi.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();

            if (CollectionUtils.isEmpty(spuEntities)) {
                break;
            }
            System.out.println(spuEntities.size());
            //遍历所有spu查询其下的所有sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsApi.querySkuBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    List<Goods> goodsList = skuEntities.stream().map(sku -> {
                        Goods goods = new Goods();
                        //创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());
                        //sku相关信息
                        goods.setSkuId(sku.getId());
                        goods.setDefaultImages(sku.getDefaultImage());
                        goods.setTitle(sku.getTitle());
                        goods.setPrice(sku.getPrice().doubleValue());
                        goods.setSubTitle(sku.getSubtitle());
                        //获取库存信息、销量、是否有货
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.gmallWmsApi.queryWareSkuEntityById(sku.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            Long sales = wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get();
                            goods.setSales(sales);
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                        }

                        //品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsApi.queryBrandById(sku.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        //分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsApi.queryCategoryById(sku.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        //检索参数
                        List<SearchAttrValue> attrValues = new ArrayList<>();
                        //注入spu_attr_value值
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = this.gmallPmsApi.querySearchSpuAttrValueByCidAndSpuId(sku.getCategoryId(), sku.getSpuId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVo.getData();
                        if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            attrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }

                        //注入sku_attr_value值

                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.gmallPmsApi.querySearchSkuAttrValueByCidAndSkuId(sku.getCategoryId(), sku.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                        if(CollectionUtils.isEmpty(skuAttrValueEntities)){
                            attrValues.addAll(skuAttrValueEntities.stream().map(
                                    skuAttrValueEntity -> {
                                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                                        BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValue);
                                        return searchAttrValue;
                                    }
                            ).collect(Collectors.toList()));
                        }
                        //检索参数分装
                        goods.setSearchAttrs(attrValues);
                        return goods;
                    }).collect(Collectors.toList());
                    this.repository.saveAll(goodsList);
                }

            });
            pageSize = spuEntities.size();
            pageNum++;
        } while (pageSize == 100);
    }

}
