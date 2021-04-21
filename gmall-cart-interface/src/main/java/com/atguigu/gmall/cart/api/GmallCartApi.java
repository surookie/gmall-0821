package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/20 14:35
 */
public interface GmallCartApi {
    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId") Long userId);
}
