package com.artezo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminUserDetailsService adminUserDetailsService;

    // ── Inject expiration so cookie maxAge matches token expiry ──
    @Value("${jwt.expiration}")
    private Long accessTokenExpiration;   // e.g. 900000  (15 min in ms)

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;  // e.g. 604800000 (7 days in ms)

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AdminUserDetailsService adminUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminUserDetailsService = adminUserDetailsService;
        logger.info("AuthController initialized");
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /api/admin/auth/login
    //  CHANGED: token no longer returned in body → set as HttpOnly cookie
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request,
                                   HttpServletResponse response) {

        String mobile = request.get("mobile");
        String password = request.get("password"); // never log this!

        logger.info("Login attempt for mobile: {}", mobile);

        if (mobile == null || mobile.trim().isEmpty()) {
            logger.warn("Login attempt with missing mobile number");
            return ResponseEntity.badRequest().body("Mobile number is required");
        }

        try {
            logger.debug("Authenticating user: {}", mobile);
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(mobile, password)
            );

            logger.debug("Authentication successful, loading user details for: {}", mobile);
            final UserDetails userDetails = adminUserDetailsService.loadUserByUsername(mobile);

            // ── Generate access token (short-lived: 15 min) ──
            final String accessToken = jwtUtil.generateToken(userDetails);

            // ── Generate refresh token (long-lived: 7 days) ──
            final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // ── Set access token as HttpOnly cookie ──
            ResponseCookie accessCookie = ResponseCookie.from("admin_token", accessToken)
                    .httpOnly(true)                          // JS cannot read this
                    .secure(true)                            // HTTPS only (set false in local dev)
                    .sameSite("Strict")                      // CSRF protection
                    .path("/")                               // Available to all paths
                    .maxAge(Duration.ofMillis(accessTokenExpiration))
                    .build();

            // ── Set refresh token as HttpOnly cookie (restricted path) ──
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/api/admin/auth/refresh")         // Only sent to refresh endpoint
                    .maxAge(Duration.ofMillis(refreshTokenExpiration))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            logger.info("Login successful for mobile: {} | Cookies set", mobile);

            // ── Return only non-sensitive info in body (NO token!) ──
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "mobile", mobile
            ));

        } catch (BadCredentialsException e) {
            logger.warn("Login failed - invalid credentials for mobile: {}", mobile);
            return ResponseEntity.status(401).body("Invalid mobile or password");

        } catch (Exception e) {
            logger.error("Unexpected error during login for mobile: {}", mobile, e);
            return ResponseEntity.status(500).body("Authentication error - please try again later");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /api/admin/auth/refresh
    //  NEW: silently issues a new access token using the refresh token cookie
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(jakarta.servlet.http.HttpServletRequest request,
                                     HttpServletResponse response) {

        logger.info("Token refresh request received");

        // ── Read refresh token from HttpOnly cookie ──
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            logger.warn("Refresh attempt with no refresh_token cookie");
            return ResponseEntity.status(401).body("Refresh token missing");
        }

        try {
            // ── Validate refresh token and extract username ──
            String mobile = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = adminUserDetailsService.loadUserByUsername(mobile);

            if (!jwtUtil.validateToken(refreshToken, userDetails)) {
                logger.warn("Invalid or expired refresh token for: {}", mobile);
                return ResponseEntity.status(401).body("Refresh token invalid or expired");
            }

            // ── Issue new short-lived access token ──
            String newAccessToken = jwtUtil.generateToken(userDetails);

            ResponseCookie newAccessCookie = ResponseCookie.from("admin_token", newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofMillis(accessTokenExpiration))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());

            logger.info("Access token refreshed successfully for: {}", mobile);
            return ResponseEntity.ok(Map.of("message", "Token refreshed"));

        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(401).body("Token refresh failed");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  POST /api/admin/auth/logout
    //  NEW: clears both cookies by setting maxAge to 0
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        logger.info("Logout request received - clearing auth cookies");

        // ── Expire access token cookie ──
        ResponseCookie clearAccess = ResponseCookie.from("admin_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)           // Immediately expires
                .build();

        // ── Expire refresh token cookie ──
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/admin/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        logger.info("Logout successful - cookies cleared");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}