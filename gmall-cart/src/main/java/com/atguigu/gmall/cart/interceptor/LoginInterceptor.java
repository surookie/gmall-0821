package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * @Description 自定义拦截器
 * @Author rookie
 * @Date 2021/4/18 11:29
 */
@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    // 全局变量的状态字段（携带重要信息的字段），在高并发的情况下，容易造成安全问题，不推荐使用。
    //public static String userId;
    // 储存在request也行，但是不够优雅，每次使用都需要把request传递过去
    public static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * @description: preHandler 在handler业务逻辑执行之前执行
     * @param request
     * @param response
     * @param handler
     * @return boolean
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("拦截器的前置方法。。。。。。。判断用户是否登录，获取userInfo,没有就生成随机userKey");

        UserInfo userInfo = new UserInfo();
        // 获取userKey
        String userKey = CookieUtils.getCookieValue(request, properties.getUserKey());
        if (StringUtils.isBlank(userKey)) {
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, properties.getUserKey(), userKey, properties.getExpire());
        }

        userInfo.setUserKey(userKey);

        // cookie，判断用户（信息放在cookie中）是否登录，
        String token = CookieUtils.getCookieValue(request, properties.getCookieName());
        if(StringUtils.isNotBlank(token)) {
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());
            userInfo.setUserId(Long.valueOf(map.get("userId").toString()));
        }

        THREAD_LOCAL.set(userInfo);
        // 拦截器  返回为true 放行， 反之
        return true;
    }

    /**
     * @description: afterCompletion的方法在视图渲染之后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @return void
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须要做的步骤，tomcat的线程池的线程不会结束（ThreadLocal会在线程结束是gc掉），数据会一直储存，地址也一直存在（引用为空，gc掉了）会容易造成内存泄露
        THREAD_LOCAL.remove();
    }

    public static UserInfo getUserInfo() {
        // key为ThreadLocal对象本身
        return THREAD_LOCAL.get();
    }


}