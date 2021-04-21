package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.cart.fegin.GmallPmsClient;
import com.atguigu.gmall.cart.fegin.GmallSmsClient;
import com.atguigu.gmall.cart.fegin.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/18 13:08
 */
@Service
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

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

    @Autowired
    private CartAsyncService cartAsyncService;

    @Override
    public void saveCart(Cart cart) {
        // 获取用户信息
        String userId = this.getUserId();
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
            this.cartAsyncService.updateCart(userId, cart, skuId);
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
            this.cartAsyncService.insertCart(userId, cart);

            // 加入购物车时， 加入价格缓存
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, cart.getPrice().toString());
        }

    }

    @Override
    public void updateNum(Cart cart) {
        this.updateCartNumAndStatus(cart);
    }

    @Override
    public void updateStatus(Cart cart) {
        this.updateCartNumAndStatus(cart);
    }

    @Override
    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOps.values();

        if(CollectionUtils.isEmpty(cartJsons)){
            return null;
        }

        return cartJsons.stream()
                .map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class))
                .filter(Cart::getCheck)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCartBySkuId(Long skuId) {
        String userId = this.getUserId();

        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if(!hashOps.hasKey(skuId.toString())){
            throw new CartException("该用户对应的购物车数据不存在！");
        }

        hashOps.delete(skuId.toString());
        this.cartAsyncService.deleteCartBySkuId(userId, skuId);
    }

    private void updateCartNumAndStatus(Cart cart) {
        String userId = this.getUserId();

        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        String skuId = cart.getSkuId().toString();
        if(!hashOps.hasKey(skuId)){
            throw new CartException("该用户对应的购物车数据不存在！");
        }
        // 获取页面传来的修改后的数据count
        BigDecimal count = cart.getCount();
        Boolean check = cart.getCheck();

        String cartJson = hashOps.get(skuId).toString();
        cart = JSON.parseObject(cartJson, Cart.class);

        cart.setCount(count);
        cart.setCheck(check);
        hashOps.put(skuId, JSON.toJSONString(cart));
        // 异步传入mysql
        this.cartAsyncService.updateCart(userId, cart, skuId);
    }

    // 获取userId
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

    @Override
    public List<Cart> queryCarts() {
        // 1. 查询用户的登录状态，先获取userInfo的userKey
        UserInfo userInfo = LoginInterceptor.THREAD_LOCAL.get();
        String userKey = userInfo.getUserKey();

        // 组装key
        String unLoginKey = KEY_PREFIX + userKey;

        // 2. 根据userKey的信息获取未登录的购物车信息
        // 查询redis，获取内层map
        BoundHashOperations<String, Object, Object> unHashOps = redisTemplate.boundHashOps(unLoginKey);
        List<Cart> unLoginCarts = null;
        List<Object> unLogincartJsons = unHashOps.values();
        if (!CollectionUtils.isEmpty(unLogincartJsons)) {
            unLoginCarts = unLogincartJsons.stream().map(cartJson ->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        // 3. 查询userId
        Long userId = userInfo.getUserId();

        // 4. 为空，直接返回
        if (userId == null) {
            return unLoginCarts;
        }
        // 5. userId获取登录的购物车信息
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);

        // 6. 未登陆与登录购物车合并
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)) {
                    // 用户的购物车包含了该记录，合并数量
                    String loginCartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(loginCartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    // 写入redis， 异步写入mysql
                    loginHashOps.put(skuId, JSON.toJSONString(cart));
                    this.cartAsyncService.updateCart(userId.toString(), cart, skuId);
                } else {
                    // 用户的购物车不包含该记录，新增记录
                    // userId覆盖数据userKey
                    cart.setUserId(userId.toString());
                    loginHashOps.put(skuId, JSON.toJSONString(cart));
                    this.cartAsyncService.insertCart(userId.toString(), cart);
                }
            });
        }

        // 7. 删除未登录购物车信息
        this.redisTemplate.delete(unLoginKey);
        this.cartAsyncService.deleteCart(userKey);

        // 8. 返回登录状态的购物车信息
        List<Object> loginCartJsons = loginHashOps.values();

        List<Cart> loginCarts = null;
        if(!CollectionUtils.isEmpty(loginCartJsons)) {
            loginCarts = loginCartJsons.stream()
                    .map(cartJson -> {
                        Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                        cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                        return cart;
                    })
                    .collect(Collectors.toList());
        }
        return loginCarts;
    }



    @Override
    @Async
    public void executor3() {
        int i = 10 / 0;
    }

    @Override
    @Async
    public Future<String> executor1() {
        System.out.println("executor1.............");
        try {
            TimeUnit.SECONDS.sleep(4);
            int i = 10 / 0;
            return AsyncResult.forValue("hello executor1");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }

    }

    @Override
    @Async
    public ListenableFuture<String> executor2() {

        try {
            int i = 10 / 0;
            return AsyncResult.forValue("hello executor1");
        } catch (Exception e) {
            // e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }
    }
}