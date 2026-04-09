package com.artezo.security;

import com.artezo.dto.request.UserLoginRequest;
import com.artezo.dto.response.UserLoginResponse;
import com.artezo.entity.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

@RestController
@RequestMapping("/api/users/auth")
public class UserAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public UserAuthController(@Qualifier("userAuthenticationManager") AuthenticationManager authenticationManager,
                              UserDetailsServiceImpl userDetailsService,
                              JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService    = userDetailsService;
        this.jwtUtil               = jwtUtil;
        logger.info("UserAuthController initialized");
    }

    private ResponseCookie buildCookie(String name, String value, String path, long maxAgeMs) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "Strict" : "Lax")
                .path(path)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "Strict" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request,
                                   HttpServletResponse response) {

        String identifier = request.getIdentifier();
        logger.info("User login attempt for identifier: {}", identifier);

        if (identifier == null || identifier.isBlank()) {
            logger.warn("Login attempt with missing identifier");
            return ResponseEntity.badRequest().body("Email or phone is required");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
            UserEntity  user        = ((UserPrincipal) userDetails).getUser();

            String accessToken  = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            response.addHeader(HttpHeaders.SET_COOKIE,
                    buildCookie("user_token",    accessToken,  "/", accessTokenExpiration).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                    buildCookie("refresh_token", refreshToken, "/", refreshTokenExpiration).toString());

            logger.info("User login successful: userId={}, secure={}", user.getUserId(), cookieSecure);

            UserLoginResponse loginResponse = new UserLoginResponse(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName()
            );

            return ResponseEntity.ok(loginResponse);

        } catch (BadCredentialsException e) {
            logger.warn("User login failed - bad credentials for identifier: {}", identifier);
            return ResponseEntity.status(401).body("Invalid email/phone or password");
        } catch (Exception e) {
            logger.error("Unexpected error during user login for identifier: {}", identifier, e);
            return ResponseEntity.status(500).body("Authentication error - please try again later");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(jakarta.servlet.http.HttpServletRequest request,
                                     HttpServletResponse response) {

        logger.info("User token refresh request received");

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
            logger.warn("User refresh attempt with no refresh_token cookie");
            return ResponseEntity.status(401).body("Refresh token missing");
        }

        try {
            String      identifier  = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

            if (!jwtUtil.validateToken(refreshToken, userDetails)) {
                logger.warn("Invalid or expired refresh token for user: {}", identifier);
                return ResponseEntity.status(401).body("Refresh token invalid or expired");
            }

            String newAccessToken = jwtUtil.generateToken(userDetails);

            response.addHeader(HttpHeaders.SET_COOKIE,
                    buildCookie("user_token", newAccessToken, "/", accessTokenExpiration).toString());

            logger.info("User access token refreshed for: {}", identifier);
            return ResponseEntity.ok("Token refreshed");

        } catch (Exception e) {
            logger.error("Error during user token refresh", e);
            return ResponseEntity.status(401).body("Token refresh failed");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        logger.info("User logout - clearing cookies");

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie("user_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString());

        logger.info("User logout successful");
        return ResponseEntity.ok("User logged out successfully");
    }
}