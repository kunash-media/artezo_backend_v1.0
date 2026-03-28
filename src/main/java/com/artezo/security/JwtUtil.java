package com.artezo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;                 // short-lived: e.g. 900000 (15 min)

    // ── NEW: separate expiration for refresh token ──
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;          // long-lived: e.g. 604800000 (7 days)

    private SecretKey getSigningKey() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        logger.trace("Signing key initialized (algorithm: {})", key.getAlgorithm());
        return key;
    }

    /**
     * Extracts the username (subject) from the JWT token.
     */
    public String extractUsername(String token) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            logger.debug("Extracted subject (username) from token: {}", username);
            return username;
        } catch (Exception e) {
            logger.warn("Failed to extract subject from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts the expiration date from the JWT token.
     */
    public Date extractExpiration(String token) {
        try {
            Date exp = extractClaim(token, Claims::getExpiration);
            logger.debug("Extracted expiration date: {}", exp);
            return exp;
        } catch (Exception e) {
            logger.warn("Failed to extract expiration from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generic method to extract any claim from the token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies all claims from the token.
     */
    private Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            logger.trace("Token claims successfully parsed. Subject: {}, Issued: {}, Expires: {}",
                    claims.getSubject(), claims.getIssuedAt(), claims.getExpiration());

            return claims;

        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired at {}: {}", e.getClaims().getExpiration(), e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error parsing JWT token", e);
            throw e;
        }
    }

    /**
     * Checks if the token has expired.
     */
    private boolean isTokenExpired(String token) {
        boolean expired = extractExpiration(token).before(new Date());
        if (expired) {
            logger.debug("Token is expired");
        } else {
            logger.trace("Token is still valid");
        }
        return expired;
    }

    /**
     * Generates a SHORT-LIVED access token (uses jwt.expiration).
     * Same as before — no breaking change.
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, expiration, "access");
    }

    /**
     * NEW: Generates a LONG-LIVED refresh token (uses jwt.refresh-expiration).
     * Stored in HttpOnly cookie with restricted path.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");   // distinguish from access token
        return buildToken(userDetails, refreshExpiration, "refresh", extraClaims);
    }

    /**
     * Internal builder — shared by generateToken and generateRefreshToken.
     */
    private String buildToken(UserDetails userDetails, Long expiryMs, String tokenType) {
        return buildToken(userDetails, expiryMs, tokenType, new HashMap<>());
    }

    private String buildToken(UserDetails userDetails, Long expiryMs, String tokenType,
                              Map<String, Object> extraClaims) {
        String username = userDetails.getUsername();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryMs);

        String token = Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        logger.info("Generated {} token for user: {} | expires at {}", tokenType, username, expiryDate);

        // Safe logging - never log full token
        String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
        logger.debug("Generated {} token preview: {}", tokenType, tokenPreview);

        return token;
    }

    /**
     * Validates if the token is valid and belongs to the given user.
     * Works for BOTH access and refresh tokens.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);

            if (!username.equals(userDetails.getUsername())) {
                logger.warn("Token subject mismatch. Expected: {}, Found in token: {}",
                        userDetails.getUsername(), username);
                return false;
            }

            if (isTokenExpired(token)) {
                logger.warn("Token expired for user: {}", username);
                return false;
            }

            logger.info("JWT token successfully validated for user: {}", username);
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("Token validation failed - expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage(), e);
            return false;
        }
    }
}