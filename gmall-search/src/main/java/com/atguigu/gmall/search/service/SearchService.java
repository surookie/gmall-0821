package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;

/**
 * @author rookie
 * @Description: 根据请求参数将查询的结果集封装到自定义的响应类中，渲染到页面
 */
public interface SearchService {

    SearchResponseVo search(SearchParamVo searchParamVo);
}
