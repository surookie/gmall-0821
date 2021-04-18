package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.TemplateEngine;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/15 14:26
 */
@Controller
public class ItemController {
    @Autowired
    private ItemService itemService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    // 生成
    @GetMapping("{skuId}.html")
    public String toItem(@PathVariable("skuId")Long skuId, Model model) {
        ItemVo itemVo = this.itemService.itemData(skuId);
        model.addAttribute("itemVo",itemVo);
        threadPoolExecutor.execute(() -> {
            // 异步生成静态页面
            itemService.generateHtmlAsync(itemVo);
        });
        return "item";
    }
}