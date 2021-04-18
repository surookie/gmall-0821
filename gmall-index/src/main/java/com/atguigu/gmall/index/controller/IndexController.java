package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
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
    public String toIndex(Model model, HttpServletRequest request){
        /*String userId = request.getHeader("userId");
        System.out.println("----------------userId = " + userId);*/
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

    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo<Object> testLock() {
        this.indexService.testLock();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo<Object> testReadLock() {
        this.indexService.testReadLock();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/writer")
    @ResponseBody
    public ResponseVo<Object> testWriterLock() {
        this.indexService.testWriterLock();
        return ResponseVo.ok();
    }
    @GetMapping("index/test/monitor")
    @ResponseBody
    public ResponseVo<Object> monitorCountDownLock() {
        try {
            this.indexService.monitorCountDownLock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResponseVo.ok();
    }
    @GetMapping("index/test/stu")
    @ResponseBody
    public ResponseVo<Object> stuCountDownLock() {
        this.indexService.stuCountDownLock();
        return ResponseVo.ok();
    }
    @GetMapping("index/test/semaphore")
    @ResponseBody
    public ResponseVo<Object> testSemaphore() {
        this.indexService.testSemaphore();
        return ResponseVo.ok();
    }
}