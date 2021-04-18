package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 手动确认
 * @Author rookie
 * @Date 2021/4/11 11:43
 */
@Component
public class GoodsListener {

    @Autowired
    private GmallPmsApi gmallPmsApi;


    @Autowired
    private GmallWmsApi gmallWmsApi;

    @Autowired
    private GoodsRepository repository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_INSERT_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        // 如果为空，则直接手动消费掉
        if(spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 查询spuId
        ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsApi.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();

        // 如果查询不到，就把手动把消息消费掉
        if(spuEntity == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 查询spu 对应的所有sku
        ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsApi.querySkuBySpuId(spuId);
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
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
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

        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}