package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-16 18:05:19
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
