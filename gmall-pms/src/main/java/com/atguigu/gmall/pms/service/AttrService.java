package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.AttrEntity;

import java.util.Map;

/**
 * 商品属性
 *
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-02 12:45:47
 */
public interface AttrService extends IService<AttrEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

