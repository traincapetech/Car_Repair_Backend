package com.carservice.backend.security.jwt;

import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        /*
         * No Authorization header or non-Bearer token.
         *
         * We do not reject here; Spring Security decides later
         * whether the target endpoint requires authentication.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            /*
             * Only ACCESS tokens can authenticate API requests.
             * Refresh tokens or tokens with any other type must be rejected immediately.
             */
            if (!jwtService.isAccessToken(token)) {
                log.debug("Token rejected: not an ACCESS token");
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            /*
             * Only authenticate if email exists and SecurityContext has no authentication yet.
             */
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {

                    User user = userOptional.get();

                    /*
                     * Inactive or deactivated users must never be authenticated.
                     */
                    if (!Boolean.TRUE.equals(user.getIsActive())) {
                        log.debug("Token rejected: user account is inactive for user ID {}", user.getId());
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (jwtService.isTokenValid(token, user.getEmail())) {

                        SimpleGrantedAuthority authority =
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        List.of(authority)
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Successfully authenticated user ID {} with role {}", user.getId(), user.getRole());
                    }
                }
            }

        } catch (Exception exception) {
            /*
             * Invalid, malformed, or expired JWT.
             * Fail safely without populating SecurityContext or leaking details.
             */
            log.debug("JWT processing failed: {}", exception.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}