package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description elasticSearch项目练手
 * @Author rookie
 * @Date 2021/4/7 23:04
 */
@Controller
@RequestMapping("search")
public class SearchController {
    @Autowired
    private SearchService searchService;
    @GetMapping
    //@ResponseBody
    public String search(SearchParamVo searchParamVo, Model model){
        SearchResponseVo responseVo = this.searchService.search(searchParamVo);
        model.addAttribute("response",responseVo);
        model.addAttribute("searchParam",searchParamVo);
        return "search";
    }

    /*@GetMapping
    @ResponseBody
    public ResponseVo<SearchResponseVo> search(SearchParamVo searchParamVo){
        SearchResponseVo responseVo = this.searchService.search(searchParamVo);
        return ResponseVo.ok(responseVo);
    }*/
}