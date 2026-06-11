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

    // General cart/wishlist reads: 60/min
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    // Write operations (add, update-quantity, remove, clear): 20/min
    private final Map<String, Bucket> writeBuckets = new ConcurrentHashMap<>();

    // Mutation-heavy ops (move-from-wishlist): 10/min
    private final Map<String, Bucket> heavyBuckets = new ConcurrentHashMap<>();

    private Bucket createGeneralBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createWriteBucket() {
        // 20 writes per minute — covers rapid qty tapping
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createHeavyBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Key by IP + userId param so different users on same network
        // don't share a bucket (e.g. office/family wifi)
        String ip     = getClientIp(request);
        String userId = request.getParameter("userId");
        String key    = userId != null ? ip + ":" + userId : ip;

        String path   = request.getRequestURI();
        String method = request.getMethod();

        Bucket bucket = resolveBucket(path, method, key);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.addHeader("Retry-After", "20");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please wait a moment.\",\"retryAfter\":20}"
            );
        }
    }

    private Bucket resolveBucket(String path, String method, String key) {
        // Heavy mutations
        if (path.endsWith("/cart/move-from-wishlist")) {
            return heavyBuckets.computeIfAbsent(key + ":heavy", k -> createHeavyBucket());
        }

        // Write operations — THIS is what was missing for update-quantity
        boolean isWrite = path.endsWith("/cart/add")
                || path.endsWith("/cart/update-quantity")   // ← was not here before
                || path.endsWith("/cart/remove")
                || path.endsWith("/cart/remove-checkout-items")
                || path.endsWith("/cart/clear")
                || path.endsWith("/wishlist/add")
                || path.endsWith("/wishlist/remove");

        if (isWrite) {
            return writeBuckets.computeIfAbsent(key + ":write", k -> createWriteBucket());
        }

        // GET endpoints (fetch cart, count, session)
        return generalBuckets.computeIfAbsent(key + ":general", k -> createGeneralBucket());
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

    @Scheduled(fixedDelay = 3_600_000) // cleanup every hour
    public void cleanupBuckets() {
        generalBuckets.clear();
        writeBuckets.clear();
        heavyBuckets.clear();
    }
}