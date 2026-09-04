package com.ricardosenna.bankingapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private record Window(Instant startedAt, AtomicInteger count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI(); int limit = path.equals("/api/auth/login") ? 5 : (path.startsWith("/api/transactions/") ? 30 : Integer.MAX_VALUE);
        if (limit == Integer.MAX_VALUE) { chain.doFilter(request, response); return; }
        String key = path + ":" + request.getRemoteAddr(); Instant now = Instant.now();
        Window window = windows.compute(key, (k, old) -> old == null || now.isAfter(old.startedAt().plusSeconds(60)) ? new Window(now, new AtomicInteger(1)) : old);
        if (window.count().getAndIncrement() >= limit) { response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); response.setHeader("Retry-After", "60"); response.setContentType("application/json"); response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}"); return; }
        chain.doFilter(request, response);
    }
}
