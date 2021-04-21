package com.atguigu.gmall.scheduled.jobHandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.entity.Cart;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description 同步数据
 * @Author rookie
 * @Date 2021/4/20 9:24
 */
@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EXCEPTION_KEY = "cart:exception:info";

    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartMapper cartMapper;

    @XxlJob("cartSyncDataJobHandler")
    public ReturnT<String> syncData(String param) {

        System.out.println("购物车定时任务：" + param);
        if (!redisTemplate.hasKey(EXCEPTION_KEY)) {
            return ReturnT.SUCCESS;
        }
        BoundSetOperations<String, String> setOps = redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();
        while (StringUtils.isNotBlank(userId)) {
            // 1.删除数据库对应userId的记录
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
            // 2.查询redis中对应的购物车记录
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
            // 3.判断是否为空
            if (hashOps.size() == 0) {
                return ReturnT.SUCCESS;
            }
            // 4. 不为空，对mysql新增数据
            List<Object> cartJsons = hashOps.values();
            cartJsons.forEach(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                this.cartMapper.insert(cart);
            });
            // 获取下一个用户进行同步
            userId = setOps.pop();
        }
        return ReturnT.SUCCESS;
    }
}