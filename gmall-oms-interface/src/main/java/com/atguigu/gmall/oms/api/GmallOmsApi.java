package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author rookie
 * @Title:
 * @Description: TODO
 * @date 2021/4/20 19:20
 */
public interface GmallOmsApi {

    @PostMapping("oms/order/save/{userId}")
    public ResponseVo saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId") Long userId);
}
