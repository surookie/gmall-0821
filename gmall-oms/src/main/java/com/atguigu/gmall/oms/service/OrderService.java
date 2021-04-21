package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderEntity;

/**
 * 订单
 *
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-20 19:08:14
 */
public interface OrderService extends IService<OrderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saveOrder(OrderSubmitVo submitVo, Long userId);
}

