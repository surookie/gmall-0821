package com.atguigu.gmall.wms.vo;

import lombok.Data;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/20 17:41
 */
@Data
public class SkuLockVo {

    private Long skuId;

    private Integer count;

    private Boolean lock; // 锁定状态

    private Long wareSkuId; // 锁成功的仓库id
}