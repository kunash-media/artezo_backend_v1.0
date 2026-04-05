package com.artezo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/auth")
public class UserAuthController {

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
    }

    // ─────────────────────────────────────────────
    // USER LOGOUT
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie("user_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString());

        return ResponseEntity.ok().body("User logged out successfully");
    }
}