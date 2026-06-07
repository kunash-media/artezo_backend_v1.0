package com.artezo.util;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> addBuckets = new ConcurrentHashMap<>(); // ← ADD

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createAddBucket() { // ← ADD
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = getClientIp(request);

        // ── Stricter bucket for add endpoints ──────────────────── ← ADD
        String path = request.getRequestURI();
        boolean isAddEndpoint = path.endsWith("/cart/add") || path.endsWith("/wishlist/add");

        Bucket bucket = isAddEndpoint
                ? addBuckets.computeIfAbsent(ip, k -> createAddBucket())
                : buckets.computeIfAbsent(ip, k -> createBucket());
        // ───────────────────────────────────────────────────────────

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}"
            );
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.contains("/api/v1/cart") || path.contains("/api/v1/wishlist"));
    }

    @Scheduled(fixedDelay = 3600000) // ← ADD: clears every hour
    public void cleanupBuckets() {
        buckets.clear();
        addBuckets.clear();
    }
}