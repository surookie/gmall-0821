package com.atguigu.gmall.index.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/12 15:02
 */
@FeignClient("pms-service")
public interface GmallPmsApi extends com.atguigu.gmall.pms.api.GmallPmsApi {

}
