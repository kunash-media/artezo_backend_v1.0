package com.artezo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String ADMIN_TOKEN = "admin_token";
    private static final String USER_TOKEN  = "user_token";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        logger.trace("JwtAuthenticationFilter processing request: {} {}", request.getMethod(), requestURI);

        // ── Step 1: Extract JWT — cookie first, then Authorization header fallback ──
        String jwt = extractTokenFromCookie(request);

        if (jwt != null) {
            logger.debug("JWT extracted from HttpOnly cookie for path: {}", requestURI);
        } else {
            jwt = extractTokenFromHeader(request, requestURI);
        }

        // ── Step 2: Extract username from token ──
        String username = null;
        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
                logger.debug("Extracted username from token: {}", username);
            } catch (Exception e) {
                logger.warn("Failed to extract username from token on path {}: {}", requestURI, e.getMessage());
            }
        }

        // ── Step 3: Authenticate if username found and no existing auth ──
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("Attempting to authenticate user: {} for path: {}", username, requestURI);

            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.trace("UserDetails loaded successfully for: {}", username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.info("Successfully authenticated user: {} via JWT for path: {}",
                            username, requestURI);
                } else {
                    logger.warn("JWT token validation failed for user: {} on path: {}",
                            username, requestURI);
                }

            } catch (Exception e) {
                logger.warn("Authentication failed for user {} on path {}: {}",
                        username, requestURI, e.getMessage(), e);
            }
        } else if (username == null) {
            logger.trace("No username extracted → skipping authentication for path: {}", requestURI);
        } else {
            logger.trace("Authentication already exists in SecurityContext → skipping for path: {}", requestURI);
        }

        // ── Step 4: Continue the filter chain ──
        filterChain.doFilter(request, response);
    }

    // ─────────────────────────────────────────────────────────────
    //  CHANGED: Read JWT from HttpOnly cookie (primary method)
    // ─────────────────────────────────────────────────────────────
//    private String extractTokenFromCookie(HttpServletRequest request) {
//        if (request.getCookies() == null) return null;
//
//        for (Cookie cookie : request.getCookies()) {
//            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
//                String value = cookie.getValue();
//                if (value != null && !value.isBlank()) {
//                    return value;
//                }
//            }
//        }
//        return null;
//    }

//    private String extractTokenFromCookie(HttpServletRequest request) {
//        if (request.getCookies() == null) return null;
//
//        String adminToken = null;
//        String userToken  = null;
//
//        for (Cookie cookie : request.getCookies()) {
//
//            if (ADMIN_TOKEN.equals(cookie.getName())) {
//                adminToken = cookie.getValue();
//            }
//
//            if (USER_TOKEN.equals(cookie.getName())) {
//                userToken = cookie.getValue();
//            }
//        }
//
//        // 🔥 PRIORITY LOGIC
//        if (adminToken != null && !adminToken.isBlank()) {
//            return adminToken; // admin takes priority
//        }
//
//        if (userToken != null && !userToken.isBlank()) {
//            return userToken;
//        }
//
//        return null;
//    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        String requestURI = request.getRequestURI();

        for (Cookie cookie : request.getCookies()) {

            // ✅ ADMIN APIs
            if (requestURI.startsWith("/api/admin") && ADMIN_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }

            // ✅ USER APIs
            if (requestURI.startsWith("/api/users") && USER_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  KEPT: Authorization header fallback (for API clients / Postman)
    // ─────────────────────────────────────────────────────────────
    private String extractTokenFromHeader(HttpServletRequest request, String requestURI) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            logger.debug("Bearer token found in Authorization header (length: {}) for path: {}",
                    jwt.length(), requestURI);
            return jwt;
        }

        logger.trace("No token found in cookie or Authorization header for path: {}", requestURI);
        return null;
    }
}