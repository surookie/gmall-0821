package com.atguigu.gmall.order.fegin;

import com.atguigu.gmall.oms.api.GmallOmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/21 15:34
 */
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {
}
