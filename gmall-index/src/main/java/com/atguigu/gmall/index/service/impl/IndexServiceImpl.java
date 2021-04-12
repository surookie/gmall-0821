package com.atguigu.gmall.index.service.impl;

import com.atguigu.gmall.index.feign.GmallPmsApi;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description 首页分页业务逻辑实现
 * @Author rookie
 * @Date 2021/4/12 15:07
 */
@Service
public class IndexServiceImpl implements IndexService {


    @Autowired
    private GmallPmsApi gmallPmsApi;

    @Override
    public List<CategoryEntity> queryLv1Categories() {
        return this.gmallPmsApi.queryCategory(0l).getData();
    }

    @Override
    public List<CategoryEntity> queryLv2CategoriesWithSubsByPid(Long pid) {
        return this.gmallPmsApi.queryLv2CategoriesWithSubsByPid(pid).getData();
    }
}