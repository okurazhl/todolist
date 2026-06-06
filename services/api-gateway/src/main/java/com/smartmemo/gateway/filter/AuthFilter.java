package com.smartmemo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 鉴权全局过滤器（占位）。
 * 当前直接放行所有请求，MVP Step 2 中实现 JWT 校验。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO: MVP Step 2 实现 JWT Token 校验
        // 1. 从 Authorization 头提取 Bearer Token
        // 2. 校验 Token 有效性
        // 3. 将用户信息写入请求头传递给下游服务
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在 LoggingFilter 之后执行
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
