package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.fegin.GmallPmsClient;
import com.atguigu.gmall.item.fegin.GmallSmsClient;
import com.atguigu.gmall.item.fegin.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/15 14:26
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public ItemVo itemData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 获取sku相关信息
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return null;
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, threadPoolExecutor);

        // 设置分类信息
        CompletableFuture<Void> categoryFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoriesByCid3 = this.pmsClient.query123CategoriesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categories = categoriesByCid3.getData();
            itemVo.setCategories(categories);
        }, threadPoolExecutor);

        // 设置品牌信息
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        // 设置spu信息
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        // 设置图片列表信息
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> skuImagesEntityResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> imagesEntities = skuImagesEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(imagesEntities)) {
                itemVo.setImages(imagesEntities);
            }
        }, threadPoolExecutor);

        // 营销信息
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = itemSaleResponseVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                itemVo.setSales(itemSaleVos);
            }
        }, threadPoolExecutor);

        // 库存信息
        CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = this.wmsClient.queryWareSkuEntityById(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        // 当前sku下的spu下的所有sku的销售信息列表
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrValueVoResponseVo = this.pmsClient.querySaleAttrBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrValueVoResponseVo.getData();
            if (!CollectionUtils.isEmpty(saleAttrValueVos)) {
                itemVo.setSaleAttrs(saleAttrValueVos);
            }
        }, threadPoolExecutor);

        // 当前sku的销售参数
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueEntityResponseVo = this.pmsClient.querySkuAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

        // 销售属性组合与skuId的映射关系
        CompletableFuture<Void> skuJsonFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> stringResponseVo = this.pmsClient.querySkuIdMappingBySpuId(skuEntity.getSpuId());
            String json = stringResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);

        // 商品描述信息,海报信息
        CompletableFuture<Void> spuImagesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null && StringUtils.isNotBlank(spuDescEntity.getDecript())) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        // 规格参数,
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> responseVo = this.pmsClient.queryGroupWithAttrValuesBy(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = responseVo.getData();
            if (!CollectionUtils.isEmpty(itemGroupVos)) {
                itemVo.setGroups(itemGroupVos);
            }
        }, threadPoolExecutor);

        //等待所有子线程完成，才能返回结果
        skuFuture.allOf(categoryFuture, brandFuture, spuFuture, imagesFuture, salesFuture,
                storeFuture, saleAttrsFuture, saleAttrFuture, skuJsonFuture, spuImagesFuture, groupFuture).join();

        return itemVo;
    }

    @Override
    public void generateHtmlAsync(ItemVo itemVo) {
        // 初始化上下文对象 ，通过该对象给模板传递渲染所需要的数据
        Context context = new Context();
        context.setVariable("itemVo", itemVo);
        // 初始化文件流
        try (PrintWriter printWriter = new PrintWriter("D:\\IdeaProjects\\project-rookie\\html\\" + itemVo.getSkuId() + ".html");) {
            templateEngine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}