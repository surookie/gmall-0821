package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("http://manage.gmall.com");//允许跨域访问的url地址
        corsConfiguration.addAllowedMethod("*");//允许跨域访问的请求方法方式，*代表所有请求方法
        corsConfiguration.addAllowedHeader("*");//允许跨域访问携带的头信息，*代表所有头信息
        corsConfiguration.setAllowCredentials(true);//允许携带cookie
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(configurationSource);
    }
}
