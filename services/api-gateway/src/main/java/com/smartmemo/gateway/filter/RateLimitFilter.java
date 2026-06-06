package com.smartmemo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 限流全局过滤器（占位）。
 * 后续通过 Redis 实现令牌桶或滑动窗口限流。
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO: 集成 Redis 实现限流
        // 1. 按 IP / 用户 / API 维度计数
        // 2. 超限返回 429 Too Many Requests
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}
