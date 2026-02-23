package com.carddemo.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration replacing RACF-based security in the mainframe.
 *
 * COBOL → Java security mapping:
 *   RACF user authentication  → Spring Security + JWT
 *   CICS COMMAREA session     → Stateless JWT tokens
 *   COSGN00C signon logic     → AuthController + AuthService
 *
 * Phase 2: Basic configuration with permit-all for development.
 * Phase 3: Full JWT filter chain implementation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Admin endpoints require ADMIN role (Phase 3: enforce via JWT)
                        .requestMatchers("/api/admin/**").permitAll()
                        // All other endpoints require authentication (Phase 3: enforce via JWT)
                        .anyRequest().permitAll()
                );

        // TODO Phase 3: Add JWT authentication filter
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
