package com.carservice.backend.common.config;

import com.carservice.backend.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                /*
                 * REST API → CSRF disabled.
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * We are using JWT instead of HTTP session.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Disable browser-based authentication mechanisms.
                 * We don't want login forms or HTTP Basic.
                 */
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                /*
                 * Public endpoints.
                 */
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/**"
                        ).permitAll()

                        /*
                         * Everything else requires JWT authentication.
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * Run JWT authentication before Spring's
                 * UsernamePasswordAuthenticationFilter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}