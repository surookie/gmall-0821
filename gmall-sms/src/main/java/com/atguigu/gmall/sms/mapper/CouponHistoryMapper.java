package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-02 14:11:47
 */
@Mapper
public interface CouponHistoryMapper extends BaseMapper<CouponHistoryEntity> {
	
}
