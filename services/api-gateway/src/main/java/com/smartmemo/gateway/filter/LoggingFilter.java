package com.smartmemo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求日志全局过滤器。
 * 记录每个请求的方法、路径、状态码和耗时。
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;
                    log.info("{} {} {} {}ms", method, path, status, duration);
                });
    }

    @Override
    public int getOrder() {
        // 在 TraceIdFilter 之后执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
