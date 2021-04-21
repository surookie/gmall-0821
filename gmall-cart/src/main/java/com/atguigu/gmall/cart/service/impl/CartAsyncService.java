package com.atguigu.gmall.cart.service.impl;


import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Description 异步添加数据，强制约定，所有异步方法的第一个参数都是userId
 * @Author rookie
 * @Date 2021/4/19 11:02
 */
@Service
public class CartAsyncService {

    @Autowired
    private CartMapper cartMapper;

    @Async
    public void deleteCart(String userKey) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userKey));
    }
    @Async
    public void updateCart(String userId, Cart cart, String skuId) {
        this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
    @Async
    public void insertCart(String userId, Cart cart) {
        /*int i = 10 / 0; 测试异常时，redis缓存同步到mysql*/
        this.cartMapper.insert(cart);
    }

    @Async
    public void deleteCartBySkuId(String userId, Long skuId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}