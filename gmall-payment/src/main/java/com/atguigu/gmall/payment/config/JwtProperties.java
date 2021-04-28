package com.atguigu.gmall.payment.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
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
    private String cookieName;
    private String userKey;
    private Integer expire;

    private PublicKey publicKey;
    @PostConstruct
    public void init(){
        try {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}