package com.artezo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminUserDetailsService adminUserDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AdminUserDetailsService adminUserDetailsService, OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.adminUserDetailsService = adminUserDetailsService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;

        logger.info("SecurityConfig bean initialized with JwtFilter and UserDetailsService");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        logger.debug("BCryptPasswordEncoder bean created (default strength = 10)");
        return encoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(adminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        logger.debug("DaoAuthenticationProvider bean created with custom UserDetailsService");
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        AuthenticationManager manager = config.getAuthenticationManager();
        logger.debug("AuthenticationManager bean created from configuration");
        return manager;
    }

    // ── NEW: CORS registered inside Spring Security ──
    // Spring Security intercepts OPTIONS preflight BEFORE Spring MVC,
    // so CORS must live here — not just in CorsConfig.java (WebMvcConfigurer).
    // You can now safely DELETE CorsConfig.java.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:5501",
                "http://127.0.0.1:5501",
                "http://localhost:5502",
                "http://127.0.0.1:5502",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:8083",
                "http://127.0.0.1:8083"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // required — sends HttpOnly cookies cross-origin
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // covers all paths
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring SecurityFilterChain - stateless JWT with cookie support");

        http
                // ── NEW: wire CORS bean into Spring Security filter chain ──
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

//                .csrf(csrf -> {
//                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
//                    csrf
//                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                            .csrfTokenRequestHandler(requestHandler)
//                            .ignoringRequestMatchers(
//                                    "/api/admin/auth/login",
//                                    "/api/admin/auth/refresh",
//                                    "/api/admin/bootstrap",
//                                    "/api/orders/**",
//                                    "/api/users/**",
//                                    "/api/v1/custom-categories/**",
//                                    "/api/inventory/**",
//                                    "/api/products/**",
//                                    "/api/v1/cart/**",
//                                    "/api/v1/wishlist/**",
//                                    "/api/otp/**",
//                                    "/api/shipping-addresses/**",
//                                    "/api/recent-users/**",
//                                    "/api/coupons/**",
//                                    "/api/reviews/**",
//                                    "/api/banners/**"
//                            );
//                    logger.debug("CSRF protection enabled with CookieCsrfTokenRepository");
//                })

                // ── Uncomment for quick Apidog/Postman testing, comment for production ──
                 .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/admin/bootstrap").permitAll()
                            .requestMatchers("/api/admin/auth/**").permitAll()
                            .requestMatchers("/api/users/auth/**").permitAll()
//                            .requestMatchers("/api/users/**").authenticated()
                            .requestMatchers("/api/admin/**").authenticated()
                            .anyRequest().permitAll();

                    logger.info("Authorization rules: /api/admin/auth/** → permitAll, " +
                            "/api/admin/** → authenticated, others → permitAll");
                })


                //------ OAuth 2.0 ---------//
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler) // we will create this
                )

                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    logger.debug("Session management set to STATELESS");
                })

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized - Login required (JWT)\"}");
                    logger.warn("Unauthorized access attempt - path: {}, message: {}",
                            request.getRequestURI(), authException.getMessage());
                })
        );

        logger.info("SecurityFilterChain configuration completed successfully");
        return http.build();
    }
}