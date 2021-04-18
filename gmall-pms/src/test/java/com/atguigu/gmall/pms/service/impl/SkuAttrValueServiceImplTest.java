package com.atguigu.gmall.pms.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/15 12:33
 */
@SpringBootTest
class SkuAttrValueServiceImplTest {

    @Autowired
    private SkuAttrValueServiceImpl skuAttrValueService;
    @Test
    void querySpuMappingBySpuId() {
        String s = this.skuAttrValueService.querySpuMappingBySpuId(24l);
        System.out.println(s);
    }
}