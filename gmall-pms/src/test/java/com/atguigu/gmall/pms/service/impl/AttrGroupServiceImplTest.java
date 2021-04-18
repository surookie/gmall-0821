package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/15 13:27
 */
@SpringBootTest
class AttrGroupServiceImplTest {
    @Autowired
    private AttrGroupServiceImpl attrGroupService;

    @Test
    void queryGroupWithAttrValuesBy() {
        List<ItemGroupVo> itemGroupVos = attrGroupService.queryGroupWithAttrValuesBy(225l, 7l, 1l);
        System.out.println("itemGroupVos = " + itemGroupVos);
    }
}