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

        // 1.防重 redis
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)) {
            throw new OrderException("非法提交");
        }
        String script = "if(redis.call('get',KEYS[1]) == ARGV[1]) then return redis.call('del',KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);

        if (!flag) {
            throw new OrderException("请不要重复提交");
        }

        // 2.验总价：遍历送货清单，获取数据库的实时价格*数量，最后累加
        BigDecimal totalPrice = submitVo.getTotalPrice(); // 页面提交时，的总价格

        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("您没有要购买的商品！");
        }

        BigDecimal currentTotalPrice = items.stream().map(item -> {
            SkuEntity skuEntity = this.pmsClient.querySkuById(item.getSkuId()).getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();

        if(totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新重试！");
        }
        // 3.验库存并锁定库存，最耗时，需要分布式锁
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
        // 4.下单
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        try {
            int i = 10 / 0;
            this.omsClient.saveOrder(submitVo, userId);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.close", orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO 发生异常，立刻解锁库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
            throw new OrderException("创建订单出错，请联系后台人员解决！");
        }
        // 5.删除购物车中对应的记录：异步
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.delete", map);
    }

    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 获取用户id
        Long userId = LoginInterceptor.getUserInfo().getUserId();
        CompletableFuture<List<Cart>> cartsFuture = CompletableFuture.supplyAsync(() -> {
            // 根据userId得到购物车内商品数据，赋值到订单中
            ResponseVo<List<Cart>> cartsResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
            List<Cart> carts = cartsResponseVo.getData();
            return carts;
        }, threadPoolExecutor);

        CompletableFuture<Void> completableFuture = cartsFuture.thenAcceptAsync((carts) -> {
            if (CollectionUtils.isEmpty(carts)) {
                throw new CartException("你没有选中的购物车记录");
            }
            List<OrderItemVo> itemVos = carts.stream().map(cart -> {
                OrderItemVo itemVo = new OrderItemVo();
                // 只取购物车中的skuId和count， 因为其他数据有可能与实时数据不一致；
                itemVo.setSkuId(cart.getSkuId());
                itemVo.setCount(cart.getCount());

                CompletableFuture<Void> skuEntityFuture = CompletableFuture.runAsync(() -> {
                    // 查询sku信息
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
                    // 查询销售信息
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                    itemVo.setSaleAttrs(skuAttrValueEntities);
                }, threadPoolExecutor);

                CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
                    // 查询营销信息
                    ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
                    List<ItemSaleVo> saleVos = salesResponseVo.getData();
                    itemVo.setSales(saleVos);
                }, threadPoolExecutor);

                CompletableFuture<Void> wareSkuEntityFuture = CompletableFuture.runAsync(() -> {
                    // 查询库存信息
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
            // 根据userId获取用户的收货地址
            ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressByUserId(userId);
            List<UserAddressEntity> userAddressEntities = addressResponseVo.getData();
            if (CollectionUtils.isEmpty(userAddressEntities)) {
                throw new CartException("用户没有填写收货地址！！！");
            }
            confirmVo.setAddresses(userAddressEntities);
        }, threadPoolExecutor);

        CompletableFuture<Void> userFuture = CompletableFuture.runAsync(() -> {
            // 根据userId获取用户当前积分
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                confirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);


        // 防重，生成唯一标识，订单编号
        // 雪花算法
        String orderToken = IdWorker.getTimeId();
        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 24, TimeUnit.HOURS);
        confirmVo.setOrderToken(orderToken);

        CompletableFuture.allOf(cartsFuture, completableFuture, userAddressFuture, userFuture).join();

        return confirmVo;
    }
}