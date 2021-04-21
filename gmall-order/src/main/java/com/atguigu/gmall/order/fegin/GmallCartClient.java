package com.atguigu.gmall.order.fegin;

import com.atguigu.gmall.cart.api.GmallCartApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/20 14:53
 */
@FeignClient("cart-service")
public interface GmallCartClient extends GmallCartApi {
}
