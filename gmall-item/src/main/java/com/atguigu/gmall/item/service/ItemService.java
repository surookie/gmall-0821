package com.atguigu.gmall.item.service;

import com.atguigu.gmall.item.vo.ItemVo;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/15 14:26
 */
public interface ItemService {
    ItemVo itemData(Long skuId);

    void generateHtmlAsync(ItemVo itemVo);
}
