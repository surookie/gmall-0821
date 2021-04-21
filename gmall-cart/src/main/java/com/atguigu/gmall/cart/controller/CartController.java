package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;


import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/18 11:36
 */
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId") Long userId) {
        List<Cart> carts = this.cartService.queryCheckedCartsByUserId(userId);
        return ResponseVo.ok(carts);
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCartBySkuId(@RequestParam Long skuId) {
        this.cartService.deleteCartBySkuId(skuId);
        return ResponseVo.ok();
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart) {
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo updateStatus(@RequestBody Cart cart) {
        this.cartService.updateStatus(cart);
        return ResponseVo.ok();
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @GetMapping
    public String saveCart(Cart cart) {
        this.cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    @GetMapping("addCart.html")
    public String toCart(@RequestParam("skuId") Long skuId, Model model) {
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test() {
        return "hello interceptor" + LoginInterceptor.getUserInfo();
    }

    @GetMapping("ex1")
    @ResponseBody
    public String executor1() {
        long l = System.currentTimeMillis();
        Future<String> future1 = this.cartService.executor1();
        try {
            System.out.println("future1.get() = " + future1.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("(System.currentTimeMillis() - l) = " + (System.currentTimeMillis() - l));
        return "executor1 test";
    }

    @GetMapping("ex2")
    @ResponseBody
    public String executor2() {
        ListenableFuture<String> future2 = this.cartService.executor2();
        long l = System.currentTimeMillis();

        future2.addCallback(result -> {
            System.out.println(result);
        }, ex -> {
            System.out.println(ex);
        });

        System.out.println("(System.currentTimeMillis() - l) = " + (System.currentTimeMillis() - l));
        return "executor2 test";
    }

    /**
     * @description: 测试统一异常处理类， 只能获取非future异常的异常信息
     * @param
     * @return java.lang.String
     */
    @GetMapping("ex3")
    @ResponseBody
    public String executor3() {
        this.cartService.executor3();
        return "executor3 test";
    }
}