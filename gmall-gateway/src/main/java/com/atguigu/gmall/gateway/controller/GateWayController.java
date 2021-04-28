package com.atguigu.gmall.gateway.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/28 15:39
 */
@Controller
public class GateWayController {

    @GetMapping("test")
    @ResponseBody
    public ResponseVo test() {
        return ResponseVo.ok();
    }

}