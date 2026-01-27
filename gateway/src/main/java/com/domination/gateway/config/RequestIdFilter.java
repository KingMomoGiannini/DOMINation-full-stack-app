package com.domination.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        // 1) En request: mutar antes de continuar
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, requestId)))
                .build();

        // 2) En response: setear ANTES del commit (no en doFinally / onComplete)
        mutated.getResponse().beforeCommit(() -> {
            HttpHeaders headers = mutated.getResponse().getHeaders();
            if (!headers.containsKey(HEADER)) {
                headers.set(HEADER, requestId);
            }
            return Mono.empty();
        });

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -1000;
    }
}
