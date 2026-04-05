package com.artezo.security;

import com.artezo.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;


@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public OAuth2SuccessHandler(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // ─────────────────────────────────────────────
        // 1. CHECK USER EXISTS OR REGISTER
        // ─────────────────────────────────────────────
            userService.registerGoogleUser(name,email); // create this method

//            UserRegistrationDTO dto = new UserRegistrationDTO();
//
//            String[] parts = name.split(" ");
//            dto.setFirstName(parts[0]);
//            dto.setLastName(parts.length > 1 ? parts[1] : "User");
//
//            dto.setEmail(email);
//
//            // Google doesn't give phone → dummy or null
//            dto.setPhone("9999999999");
//
//            // random password (not used)
//            dto.setPassword(UUID.randomUUID().toString());
//
//            userService.registerUser(dto);


        // ─────────────────────────────────────────────
        // 2. GENERATE JWT (like admin)
        // ─────────────────────────────────────────────

        String accessToken  = jwtUtil.generateToken(
                org.springframework.security.core.userdetails.User
                        .withUsername(email)
                        .password("")
                        .authorities("ROLE_USER")
                        .build()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                org.springframework.security.core.userdetails.User
                        .withUsername(email)
                        .password("")
                        .authorities("ROLE_USER")
                        .build()
        );

        // ─────────────────────────────────────────────
        // 3. SET COOKIES
        // ─────────────────────────────────────────────
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("user_token", accessToken)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build().toString()
        );

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refresh_token", refreshToken)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build().toString()
        );



        // ─────────────────────────────────────────────
        // 4. REDIRECT TO FRONTEND
        // ─────────────────────────────────────────────
        response.sendRedirect("http://127.0.0.1:5500/sign-with-gogle/welcome-page.html");
    }
}