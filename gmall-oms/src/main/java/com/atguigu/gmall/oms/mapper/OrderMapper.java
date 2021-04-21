package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-20 19:08:14
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    int updateStatus(@Param("orderToken") String orderToken, @Param("expectStatus") Integer expectStatus, @Param("targetStatus") Integer targetStatus);
}
