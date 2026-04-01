package com.artezo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ShiprocketAuthService {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketAuthService.class);

    private final RestClient restClient;

    @Value("${shiprocket.email}")
    private String email;

    @Value("${shiprocket.password}")
    private String password;

    // ── In-memory token cache ─────────────────────────────────────────────
    private String cachedToken;
    private LocalDateTime tokenExpiry;

    public ShiprocketAuthService(@Qualifier("shiprocketRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Returns a valid Bearer token.
     * Fetches a new one only if cache is empty or expired.
     */
    public String getToken() {
        if (cachedToken != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            log.debug("Shiprocket token served from cache — expires at {}", tokenExpiry);
            return cachedToken;
        }
        return refreshToken();
    }

    /**
     * Force fetches a new token from Shiprocket — called on cache miss or expiry.
     */
    private String refreshToken() {
        log.info("Fetching new Shiprocket auth token...");
        log.info("┌─────────────────────────────────────────────┐");
        log.info("│   SHIPROCKET AUTH — Fetching new token...   │");
        log.info("└─────────────────────────────────────────────┘");
        log.info("► Email: {}", email);

        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        try {
            Map response = restClient.post()
                    .uri("/auth/login")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("token")) {
                throw new RuntimeException("Shiprocket auth response missing token field");
            }

            cachedToken  = (String) response.get("token");
            tokenExpiry  = LocalDateTime.now().plusHours(23); // SR token lives 24h, refresh at 23h

            log.info("Shiprocket token refreshed successfully — valid until {}", tokenExpiry);
            return cachedToken;

        } catch (Exception e) {
            log.error("Failed to fetch Shiprocket auth token: {}", e.getMessage());
            throw new RuntimeException("Shiprocket authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns "Bearer <token>" — ready to put directly in Authorization header.
     */
    public String getBearerToken() {
        return "Bearer " + getToken();
    }

    /**
     * Manually invalidate cache — call this if any API call returns 401.
     */
    public void invalidateToken() {
        log.warn("Shiprocket token manually invalidated — will refresh on next call");
        cachedToken = null;
        tokenExpiry = null;
    }
}