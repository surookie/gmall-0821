package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.order.fegin.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/20 14:57
 */
@Component
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static final String KEY_PREFIX = "order:token:";

    @Override
    public void submit(OrderSubmitVo submitVo) {

        // 1.?????? redis
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)) {
            throw new OrderException("????????????");
        }
        String script = "if(redis.call('get',KEYS[1]) == ARGV[1]) then return redis.call('del',KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);

        if (!flag) {
            throw new OrderException("?????????????????????");
        }

        // 2.???????????????????????????????????????????????????????????????*?????????????????????
        BigDecimal totalPrice = submitVo.getTotalPrice(); // ??????????????????????????????

        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("??????????????????????????????");
        }

        BigDecimal currentTotalPrice = items.stream().map(item -> {
            SkuEntity skuEntity = this.pmsClient.querySkuById(item.getSkuId()).getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();

        if(totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("????????????????????????????????????");
        }
        // 3.?????????????????????????????????????????????????????????
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        List<SkuLockVo> skuLockVos = this.wmsClient.checkAndLock(lockVos, orderToken).getData();
        if (!CollectionUtils.isEmpty(skuLockVos)) {
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }

        // 4.??????
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        try {
            this.omsClient.saveOrder(submitVo, userId);
            // ????????????
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.close", orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            // ???????????????????????????????????????????????????????????????????????????????????????
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
            throw new OrderException("???????????????????????????????????????????????????");
        }
        // 5.??????????????????????????????????????????
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.delete", map);
    }

    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // ????????????id
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        CompletableFuture<List<Cart>> cartsFuture = CompletableFuture.supplyAsync(() -> {
            // ??????userId???????????????????????????????????????????????????
            ResponseVo<List<Cart>> cartsResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
            List<Cart> carts = cartsResponseVo.getData();
            return carts;
        }, threadPoolExecutor);

        CompletableFuture<Void> completableFuture = cartsFuture.thenAcceptAsync((carts) -> {
            if (CollectionUtils.isEmpty(carts)) {
                throw new CartException("?????????????????????????????????");
            }
            List<OrderItemVo> itemVos = carts.stream().map(cart -> {
                OrderItemVo itemVo = new OrderItemVo();
                // ?????????????????????skuId???count??? ??????????????????????????????????????????????????????
                itemVo.setSkuId(cart.getSkuId());
                itemVo.setCount(cart.getCount());

                CompletableFuture<Void> skuEntityFuture = CompletableFuture.runAsync(() -> {
                    // ??????sku??????
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    if (skuEntity != null) {
                        itemVo.setTitle(skuEntity.getTitle());
                        itemVo.setDefaultImage(skuEntity.getDefaultImage());
                        itemVo.setPrice(skuEntity.getPrice());
                        itemVo.setWeight(skuEntity.getWeight());
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> skuAttrValueFuture = CompletableFuture.runAsync(() -> {
                    // ??????????????????
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                    itemVo.setSaleAttrs(skuAttrValueEntities);
                }, threadPoolExecutor);

                CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
                    // ??????????????????
                    ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
                    List<ItemSaleVo> saleVos = salesResponseVo.getData();
                    itemVo.setSales(saleVos);
                }, threadPoolExecutor);

                CompletableFuture<Void> wareSkuEntityFuture = CompletableFuture.runAsync(() -> {
                    // ??????????????????
                    ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = this.wmsClient.queryWareSkuEntityById(cart.getSkuId());
                    List<WareSkuEntity> wareSkuEntities = wareSkuEntityResponseVo.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    }
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuEntityFuture, skuAttrValueFuture, salesFuture, wareSkuEntityFuture).join();

                return itemVo;
            }).collect(Collectors.toList());
            confirmVo.setOrderItems(itemVos);
        }, threadPoolExecutor);


        CompletableFuture<Void> userAddressFuture = CompletableFuture.runAsync(() -> {
            // ??????userId???????????????????????????
            ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressByUserId(userId);
            List<UserAddressEntity> userAddressEntities = addressResponseVo.getData();
            if (CollectionUtils.isEmpty(userAddressEntities)) {
                throw new CartException("???????????????????????????????????????");
            }
            confirmVo.setAddresses(userAddressEntities);
        }, threadPoolExecutor);

        CompletableFuture<Void> userFuture = CompletableFuture.runAsync(() -> {
            // ??????userId????????????????????????
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                confirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);


        // ??????????????????????????????????????????
        // ????????????
        String orderToken = IdWorker.getTimeId();
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 24, TimeUnit.HOURS);
        confirmVo.setOrderToken(orderToken);

        CompletableFuture.allOf(cartsFuture, completableFuture, userAddressFuture, userFuture).join();

        return confirmVo;
    }
}