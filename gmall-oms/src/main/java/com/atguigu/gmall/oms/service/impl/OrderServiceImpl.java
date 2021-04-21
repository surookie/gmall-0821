package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.fegin.GmallPmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemMapper itemMapper;

    @Autowired
    private GmallPmsClient pmsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saveOrder(OrderSubmitVo submitVo, Long userId) {

        List<OrderItemVo> items = submitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("您没有购买的商品信息。。");
        }

        // 新增订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());

        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        // TODO 总金额+运费 -满减-积分抵现-打折等
        orderEntity.setPayAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0); // '订单来源[0->PC订单；1->app订单]',
        orderEntity.setStatus(0); // '订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】'
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        // TODO 遍历所有商品，积分信息累加
        orderEntity.setIntegration(1000);
        orderEntity.setGrowth(1000);

        UserAddressEntity userAddressEntity = submitVo.getAddress();
        if (userAddressEntity != null) {
            orderEntity.setReceiverAddress(userAddressEntity.getAddress());
            orderEntity.setReceiverCity(userAddressEntity.getCity());
            orderEntity.setReceiverName(userAddressEntity.getName());
            orderEntity.setReceiverPhone(userAddressEntity.getPhone());
            orderEntity.setReceiverPostCode(userAddressEntity.getPostCode());
            orderEntity.setReceiverProvince(userAddressEntity.getProvince());
            orderEntity.setReceiverRegion(userAddressEntity.getRegion());
        }
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        this.save(orderEntity);
        Long orderEntityId = orderEntity.getId();
        // 新增订单详情表
        items.forEach(item -> {
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrderId(orderEntityId);
            orderItemEntity.setOrderSn(submitVo.getOrderToken());

            //根据skuId查询sku相关信息
            SkuEntity skuEntity = this.pmsClient.querySkuById(item.getSkuId()).getData();
            if (skuEntity != null) {
                orderItemEntity.setSkuId(skuEntity.getId());
                orderItemEntity.setSkuName(skuEntity.getName());
                orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
                orderItemEntity.setSkuPrice(skuEntity.getPrice());
                orderItemEntity.setCategoryId(skuEntity.getCategoryId());
            }
            // 查询sku的营销属性
            List<SkuAttrValueEntity> skuAttrValueEntities = this.pmsClient.querySkuAttrValueBySkuId(item.getSkuId()).getData();
            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));

            // 查询品牌
            BrandEntity brandEntity = this.pmsClient.queryBrandById(skuEntity.getBrandId()).getData();
            orderItemEntity.setSpuBrand(brandEntity.getName());

            // 根据skuId查询spu相关信息
            SpuEntity spuEntity = this.pmsClient.querySpuById(skuEntity.getSpuId()).getData();
            if (spuEntity != null) {
                orderItemEntity.setSpuId(spuEntity.getId());
                orderItemEntity.setSpuName(spuEntity.getName());
            }
            // 查询spu的描述信息
            SpuDescEntity spuDescEntity = this.pmsClient.querySpuDescById(spuEntity.getId()).getData();
            if (spuDescEntity != null) {
                orderItemEntity.setSpuPic(spuDescEntity.getDecript());
            }
            // TODO：查询商品的赠送的积分信息

            this.itemMapper.insert(orderItemEntity);
        });


    }

}