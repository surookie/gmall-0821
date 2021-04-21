package com.atguigu.gmall.order.fegin;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/20 14:55
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
