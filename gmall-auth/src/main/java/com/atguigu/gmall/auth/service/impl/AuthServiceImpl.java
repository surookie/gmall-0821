package com.atguigu.gmall.auth.service.impl;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/17 10:11
 */
// 启用自定义Properties
@EnableConfigurationProperties(JwtProperties.class)
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private GmallUmsClient gmallUmsClient;

    @Autowired
    private JwtProperties properties;

    @Override
    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {

            // 1.校验用户名和密码
            ResponseVo<UserEntity> userEntityResponseVo = gmallUmsClient.queryUser(loginName, password);
            UserEntity userEntity = userEntityResponseVo.getData();

            // 2.判断用户信息是否为空
            if(userEntity == null) {
                throw new UserException("用户名或密码错误！");
            }

            // 3.组织载荷
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userEntity.getId());
            map.put("userName", userEntity.getUsername());
            map.put("ip", IpUtils.getIpAddressAtService(request));

            // 4.生成jwt
            String token = JwtUtils.generateToken(map, this.properties.getPrivateKey(), this.properties.getExpire());

            // 5.把jwt放到cookie中
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);

            // 6.为了方便展示用户信息
            CookieUtils.setCookie(request, response, this.properties.getUnick(), userEntity.getNickname(), this.properties.getExpire() *60);

    }
}