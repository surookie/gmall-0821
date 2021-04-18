package com.atguigu.gmall.ums;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/16 22:17
 */
public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\IdeaProjects\\project-rookie\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\IdeaProjects\\project-rookie\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MTg1ODM3MDh9.MJ9a_lrKFkrzuiqL1iPq7CyjKhqL8seThP_K8J_3MsGz-nSyPqYYwj7LDruuL69-OcIYmwX4oPhGrfqlQNnY_4aGXWaqrInLdxlLt7qkqDLUjl13qwCsEhIAql86Zt8FS08wG50k3oIwkqW_ofj3oEScmDheGdzj5zHZj1apddEvyiZJI3dgyN4M5ZZRaAfWvL95Sownn9Io5kdvSlkv4uxjKjCACPQA90tGQjfD6kPz4Q_FJjaGmZK4wuZp-E-A70DHqAS5Q9y01_6BoKALnOaXYslHnEoI_aq5Jb0A-mIJkBDZvMIehXCcixNVN4tVduhOcP45UXPyBHVOvL102A";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}