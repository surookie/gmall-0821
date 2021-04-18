package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import com.google.common.net.HttpHeaders;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/17 16:46
 */
@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathsConfig> {

    @Autowired
    private JwtProperties properties;

    public AuthGatewayFilterFactory() {
        super(PathsConfig.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public GatewayFilter apply(PathsConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                //System.out.println("局部过滤器。。。。。。。。。。。" + config.getPaths());

                //获取网关的request和response
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                List<String> paths = config.getPaths();
                // 当前的路径
                String curPath = request.getURI().getPath();

                //1. 获取当前的请求路径，看是否在拦截名单(为空直接放行)中，没有则直接放行
                if (CollectionUtils.isEmpty(paths) || !paths.stream().anyMatch(path -> StringUtils.startsWith(curPath, path))) {
                    return chain.filter(exchange);
                }
                //2. 获取请求中的token， 异步 头信息获取， 同步：cookie中获取
                String token = request.getHeaders().getFirst("token");
                if (StringUtils.isBlank(token)) {
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(properties.getCookieName())) {
                        token = cookies.getFirst(properties.getCookieName()).getValue();
                    }
                }

                //3. 判断token信息是否为空，为空，则重定向到登陆页面
                if(StringUtils.isBlank(token)){
                    // 重定向到登陆页面
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    // 拦截请求
                    return response.setComplete();
                }
                try {
                    //4. 使用公钥进行解密jwt，解析异常了，返回登陆页面
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

                    //5. 判断是否为自己的token，获取当前的ip的地址 比较。不一致，则重定向到登录页面
                    String curIp = IpUtils.getIpAddressAtGateway(request);
                    if(!StringUtils.equals(map.get("ip").toString(), curIp)) {
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        // 拦截请求
                        return response.setComplete();
                    }
                    //6. 把jwt的用户信息，设置到新的头信息，让其他微服务拿到
                    request.mutate().header("userId", map.get("userId").toString()).build();
                    exchange.mutate().request(request).build();
                    //7. 放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    // 拦截请求
                    return response.setComplete();
                }
            }
        };
    }

    @Data
    public static class PathsConfig {
        private List<String> paths;
        /*private String key;
        private String value;*/
    }
}