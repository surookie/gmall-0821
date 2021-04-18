package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/12 15:02
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {

}
