package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: 远程调用ums-service
 * @date 2021/4/17 10:05
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
