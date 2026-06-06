package com.smartmemo.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * TraceId 全局过滤器。
 * 每个请求生成唯一 traceId，写入 MDC 和响应头，便于全链路追踪。
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 优先使用客户端传入的 traceId，否则生成新的
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // 写入响应头
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        // 写入 MDC 供日志使用
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        return chain.filter(exchange)
                .doFinally(signalType -> MDC.remove(TRACE_ID_MDC_KEY));
    }

    @Override
    public int getOrder() {
        // 最高优先级，最先执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
