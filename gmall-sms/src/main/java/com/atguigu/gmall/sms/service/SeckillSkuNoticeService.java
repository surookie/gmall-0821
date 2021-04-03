package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SeckillSkuNoticeEntity;

import java.util.Map;

/**
 * 秒杀商品通知订阅
 *
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-02 14:11:47
 */
public interface SeckillSkuNoticeService extends IService<SeckillSkuNoticeEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

