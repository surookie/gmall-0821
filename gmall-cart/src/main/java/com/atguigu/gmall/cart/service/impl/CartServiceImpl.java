package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.cart.fegin.GmallPmsClient;
import com.atguigu.gmall.cart.fegin.GmallSmsClient;
import com.atguigu.gmall.cart.fegin.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/18 13:08
 */
@Service
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Override
    public void saveCart(Cart cart) {
        // 获取用户信息
        String userId = getUserId();
        // 获取当前用户的购物车信息，redis内存数据模型：map<userId, map<skuId, Cart的json字符串>>
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);

        // 判断该用户的购物车是否包含该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // 包含：更新数量
            String cartJson = hashOps.get(skuId).toString();

            cart = JSON.parseObject(cartJson, Cart.class);

            cart.setCount(cart.getCount().add(count));
            // 用更新的后的cart对象覆盖redis中的对象，相当于this.redisTemplate.opsForHash().put(userId, skuId, JSON.toJSONString(cart));
            hashOps.put(skuId, JSON.toJSONString(cart));
            cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", Long.valueOf(skuId)));
        } else {
            // 不包含：新增一条记录
            cart.setUserId(userId);
            // 是否勾选
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = gmallPmsClient.querySkuById(Long.valueOf(skuId));
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                // sku属性
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
            }
            // 库存属性
            ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = gmallWmsClient.queryWareSkuEntityById(Long.valueOf(skuId));
            List<WareSkuEntity> wareSkuEntities = wareSkuEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = gmallPmsClient.querySkuAttrValueBySkuId(Long.valueOf(skuId));
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            }
            // 营销属性
            ResponseVo<List<ItemSaleVo>> salesResponseVo = gmallSmsClient.querySalesBySkuId(Long.valueOf(skuId));
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }
            // 保存到redis
            hashOps.put(skuId, JSON.toJSONString(cart));
            // 保存mysql
            cartMapper.insert(cart);
        }

    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.THREAD_LOCAL.get();

        if (userInfo.getUserId() == null) {
            return userInfo.getUserKey();
        } else {
            return userInfo.getUserId().toString();
        }

    }

    @Override
    public Cart queryCartBySkuId(Long skuId) {
        String userId = this.getUserId();
        // 根据userId获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        // 根据内层的map
        if (hashOps.hasKey(skuId.toString())) {
            String cartJson = hashOps.get(skuId.toString()).toString();

            return JSON.parseObject(cartJson, Cart.class);
        }
        return null;
    }
}