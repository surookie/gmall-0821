package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.transform.Source;
import java.util.List;

/**
 * @Description 首页三级分类功能实现
 * @Author rookie
 * @Date 2021/4/12 14:59
 */
@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model){
        // 获取以及一级分类
        List<CategoryEntity> categories = this.indexService.queryLv1Categories();
        model.addAttribute("categories", categories);
        // TODO: 获取广告信息
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLv2CategoriesWithSubsByPid(@PathVariable("pid") Long pid) {
        List<CategoryEntity> categories = this.indexService.queryLv2CategoriesWithSubsByPid(pid);
        return ResponseVo.ok(categories);
    }
}