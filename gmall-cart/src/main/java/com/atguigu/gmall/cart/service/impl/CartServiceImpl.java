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
        // ??????????????????
        String userId = this.getUserId();
        // ???????????????????????????????????????redis?????????????????????map<userId, map<skuId, Cart???json?????????>>
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);

        // ????????????????????????????????????????????????
        String skuId = cart.getSkuId().toString();

        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // ?????????????????????
            String cartJson = hashOps.get(skuId).toString();

            cart = JSON.parseObject(cartJson, Cart.class);

            cart.setCount(cart.getCount().add(count));
            // ??????????????????cart????????????redis????????????????????????this.redisTemplate.opsForHash().put(userId, skuId, JSON.toJSONString(cart));
            hashOps.put(skuId, JSON.toJSONString(cart));
            this.cartAsyncService.updateCart(userId, cart, skuId);
        } else {
            // ??????????????????????????????
            cart.setUserId(userId);
            // ????????????
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = gmallPmsClient.querySkuById(Long.valueOf(skuId));
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                // sku??????
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
            }
            // ????????????
            ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = gmallWmsClient.queryWareSkuEntityById(Long.valueOf(skuId));
            List<WareSkuEntity> wareSkuEntities = wareSkuEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // ????????????
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = gmallPmsClient.querySkuAttrValueBySkuId(Long.valueOf(skuId));
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            }
            // ????????????
            ResponseVo<List<ItemSaleVo>> salesResponseVo = gmallSmsClient.querySalesBySkuId(Long.valueOf(skuId));
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }
            // ?????????redis
            hashOps.put(skuId, JSON.toJSONString(cart));
            // ??????mysql
            this.cartAsyncService.insertCart(userId, cart);

            // ????????????????????? ??????????????????
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
            throw new CartException("?????????????????????????????????????????????");
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
            throw new CartException("?????????????????????????????????????????????");
        }
        // ???????????????????????????????????????count
        BigDecimal count = cart.getCount();
        Boolean check = cart.getCheck();

        String cartJson = hashOps.get(skuId).toString();
        cart = JSON.parseObject(cartJson, Cart.class);

        cart.setCount(count);
        cart.setCheck(check);
        hashOps.put(skuId, JSON.toJSONString(cart));
        // ????????????mysql
        this.cartAsyncService.updateCart(userId, cart, skuId);
    }

    // ??????userId
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
        // ??????userId???????????????map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        // ???????????????map
        if (hashOps.hasKey(skuId.toString())) {
            String cartJson = hashOps.get(skuId.toString()).toString();

            return JSON.parseObject(cartJson, Cart.class);
        }
        return null;
    }

    @Override
    public List<Cart> queryCarts() {
        // 1. ???????????????????????????????????????userInfo???userKey
        UserInfo userInfo = LoginInterceptor.THREAD_LOCAL.get();
        String userKey = userInfo.getUserKey();

        // ??????key
        String unLoginKey = KEY_PREFIX + userKey;

        // 2. ??????userKey??????????????????????????????????????????
        // ??????redis???????????????map
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
        // 3. ??????userId
        Long userId = userInfo.getUserId();

        // 4. ?????????????????????
        if (userId == null) {
            return unLoginCarts;
        }
        // 5. userId??????????????????????????????
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);

        // 6. ?????????????????????????????????
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)) {
                    // ???????????????????????????????????????????????????
                    String loginCartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(loginCartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    // ??????redis??? ????????????mysql
                    loginHashOps.put(skuId, JSON.toJSONString(cart));
                    this.cartAsyncService.updateCart(userId.toString(), cart, skuId);
                } else {
                    // ???????????????????????????????????????????????????
                    // userId????????????userKey
                    cart.setUserId(userId.toString());
                    loginHashOps.put(skuId, JSON.toJSONString(cart));
                    this.cartAsyncService.insertCart(userId.toString(), cart);
                }
            });
        }

        // 7. ??????????????????????????????
        this.redisTemplate.delete(unLoginKey);
        this.cartAsyncService.deleteCart(userKey);

        // 8. ????????????????????????????????????
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