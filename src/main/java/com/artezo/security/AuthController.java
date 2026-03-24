package com.artezo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminUserDetailsService adminUserDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AdminUserDetailsService adminUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminUserDetailsService = adminUserDetailsService;
        logger.info("AuthController initialized");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        String password = request.get("password"); // never log this!

        logger.info("Login attempt for mobile: {} from IP: {}",
                mobile, getClientIp(request)); // optional: add IP if useful

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

            logger.debug("Generating JWT token for: {}", mobile);
            final String jwt = jwtUtil.generateToken(userDetails);

            logger.info("Login successful for mobile: {} | Token generated", mobile);

            return ResponseEntity.ok(Map.of(
                    "token", jwt,
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

    // Optional helper: get client IP (useful for security monitoring)
    private String getClientIp(Map<String, String> request) {
        // In real app, better to use request.getRemoteAddr() or X-Forwarded-For header
        // This is just placeholder since we only have Map here
        return "unknown";
    }
}