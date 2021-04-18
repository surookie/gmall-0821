package com.atguigu.gmall.auth.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author rookie
 * @Title:
 * @Description:
 * @date 2021/4/17 10:10
 */
public interface AuthService {
    void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response);
}
