package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/18 13:08
 */
public interface CartService {
    void saveCart(Cart cart);

    Cart queryCartBySkuId(Long skuId);
}
