package com.atguigu.gmall.payment.feign;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/28 12:22
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
