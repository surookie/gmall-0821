package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @Description 读取公钥和私钥赋值给公钥和私钥对象
 * @Author rookie
 * @Date 2021/4/16 22:50
 */
@Data
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String pubKeyPath;
    private String secret;
    private String cookieName;
    private PublicKey publicKey;
    @PostConstruct
    public void init(){
        try {
            File pubFile = new File(pubKeyPath);
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}