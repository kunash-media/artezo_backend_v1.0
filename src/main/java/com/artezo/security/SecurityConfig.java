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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity   // NEW: enables @PreAuthorize on controller methods
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminUserDetailsService adminUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AdminUserDetailsService adminUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.adminUserDetailsService = adminUserDetailsService;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring SecurityFilterChain - stateless JWT with cookie support");

        http
                // ── CHANGED: CSRF enabled now that we use cookies ──
                // Frontend must read XSRF-TOKEN cookie and send as X-XSRF-TOKEN header
//                .csrf(csrf -> {
//                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
//                    csrf
//                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                            // CookieCsrfTokenRepository sets XSRF-TOKEN cookie (readable by JS)
//                            // Frontend sends it back as X-XSRF-TOKEN header on state-changing requests
//                            .csrfTokenRequestHandler(requestHandler)
//                            // ── Exempt login & refresh from CSRF (they're pre-auth) ──
//                            .ignoringRequestMatchers(
//                                    "/api/admin/auth/login",
//                                    "/api/admin/auth/refresh",
//                                    "/api/admin/bootstrap"
//
//                            );
//                    logger.debug("CSRF protection enabled with CookieCsrfTokenRepository");
//                })
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/admin/bootstrap").permitAll()
                            .requestMatchers("/api/admin/auth/**").permitAll()   // login, logout, refresh
                            .requestMatchers("/api/admin/**").authenticated()
                            .anyRequest().permitAll();

                    logger.info("Authorization rules: /api/admin/auth/** → permitAll, " +
                            "/api/admin/** → authenticated, others → permitAll");
                })

                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    logger.debug("Session management set to STATELESS");
                })

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // ── Custom 401 response (unchanged) ──
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    String message = "{\"error\":\"Unauthorized - Login required (JWT)\"}";
                    response.getWriter().write(message);

                    logger.warn("Unauthorized access attempt - path: {}, message: {}",
                            request.getRequestURI(), authException.getMessage());
                })
        );

        logger.info("SecurityFilterChain configuration completed successfully");
        return http.build();
    }
}