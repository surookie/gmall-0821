package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-05 10:36:40
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

	List<WareSkuEntity> check(@Param("skuId") Long skuId, @Param("count") Integer count);

	Integer lock(@Param("id") Long id, @Param("count") Integer count);

	Integer unlock(@Param("wid") Long id, @Param("count") Integer count);
}
