package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;
import org.springframework.util.concurrent.ListenableFuture;


import java.util.List;
import java.util.concurrent.Future;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/18 13:08
 */
public interface CartService {
    void saveCart(Cart cart);

    Cart queryCartBySkuId(Long skuId);

    Future<String> executor1();

    ListenableFuture<String> executor2();

    void executor3();

    List<Cart> queryCarts();

    void updateNum(Cart cart);

    void updateStatus(Cart cart);

    void deleteCartBySkuId(Long skuId);

    List<Cart> queryCheckedCartsByUserId(Long userId);
}
