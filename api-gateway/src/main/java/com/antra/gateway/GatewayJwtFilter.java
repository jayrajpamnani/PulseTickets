package com.antra.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayJwtFilter implements GlobalFilter, Ordered {
  private final SecretKey key;

  GatewayJwtFilter(@Value("${app.jwt.secret:change-me-change-me-change-me-change-me}") String secret) {
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    String path = request.getURI().getPath();
    boolean publicRequest = request.getMethod() == HttpMethod.POST &&
        List.of("/api/auth/register", "/api/auth/login").contains(path);
    publicRequest |= request.getMethod() == HttpMethod.GET &&
        (path.equals("/api/events") || path.matches("/api/events/[^/]+"));
    publicRequest |= path.startsWith("/actuator/");
    if (publicRequest) return chain.filter(exchange);

    String header = request.getHeaders().getFirst("Authorization");
    if (header == null || !header.startsWith("Bearer ")) return reject(exchange);
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7));
      return chain.filter(exchange);
    } catch (RuntimeException invalidToken) {
      return reject(exchange);
    }
  }

  private Mono<Void> reject(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override public int getOrder() { return -100; }
}
