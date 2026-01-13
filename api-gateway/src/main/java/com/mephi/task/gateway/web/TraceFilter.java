package com.mephi.task.gateway.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class TraceFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String headerTraceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        final String traceId = (headerTraceId == null || headerTraceId.isBlank())
                ? UUID.randomUUID().toString()
                : headerTraceId;
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();
        ServerWebExchange mutatedEx = exchange.mutate().request(mutated).build();
        log.info("gateway incoming {} {} traceId={}", mutated.getMethod(), mutated.getURI(), traceId);
        // Trace ID передается в запрос к backend-сервису и логируется
        // Не добавляем в response, так как в WebFlux ответ может быть уже закоммичен
        return chain.filter(mutatedEx);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}


