package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;

/**
 * @author rookie
 * @Title:
 * @Description: 首页分页业务
 * @date 2021/4/12 15:01
 */

public interface IndexService {

    // 获取一级分类
    List<CategoryEntity> queryLv1Categories();

    //获取二级分类及三级分类
    List<CategoryEntity> queryLv2CategoriesWithSubsByPid(Long pid);

}
