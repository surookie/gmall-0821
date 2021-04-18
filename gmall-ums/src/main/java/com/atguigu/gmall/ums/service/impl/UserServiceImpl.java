package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.exception.UserException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );
        return new PageResultVo(page);
    }

    // 数据校验 1，用户名；2，手机；3，邮箱
    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type) {
            case 1:
                wrapper.eq("username", data);
                break;
            case 2:
                wrapper.eq("phone", data);
                break;
            case 3:
                wrapper.eq("email", data);
                break;
            default:
                return null;
        }
        return this.count() == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // TODO 1.校验短信验证码， 根据手机号查询redis中的验证码的code

        // 2.生成salt
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);
        if(StringUtils.isBlank(userEntity.getUsername()) || StringUtils.isBlank(userEntity.getPassword())){
            throw new UserException("用户名或密码不能为空！");
        }
        // 3.对密码加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        // 4.新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);
        // TODO 5.删除redis的短信验证码

    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        // 防止username 与 手机号重名， 查询所有用户
        List<UserEntity> userEntities = this.list(
                wrapper.eq("username", loginName)
                .or().eq("phone", loginName)
                .or().eq("email", loginName));
        // 判断用户名是否为空
        if(CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        for (UserEntity userEntity : userEntities) {
            // 对登录用户的密码进行加盐加密
            password = DigestUtils.md5Hex(password + userEntity.getSalt());
            // 比较密码
            if(StringUtils.equals(password,userEntity.getPassword())){
                return userEntity;
            }
        }
        return null;
    }

}