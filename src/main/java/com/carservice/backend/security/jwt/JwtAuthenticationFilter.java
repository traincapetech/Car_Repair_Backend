package com.carservice.backend.security.jwt;

import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        System.out.println(
        "JWT FILTER -> "
                + request.getMethod()
                + " "
                + request.getRequestURI()
                + " | Authorization: "
                + (authHeader != null)
);

        /*
         * No Authorization header.
         *
         * We don't immediately reject the request here.
         * Spring Security will decide later whether the
         * requested endpoint requires authentication.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            /*
             * Only ACCESS tokens can authenticate API requests.
             */
            String tokenType = jwtService.extractTokenType(token);

            if (!"ACCESS".equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            /*
             * Don't authenticate the request again if Spring Security
             * already has an authenticated user.
             */
            if (email != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                Optional<User> userOptional =
                        userRepository.findByEmail(email);

                if (userOptional.isPresent()) {

                    User user = userOptional.get();

                    if (!user.getIsActive()) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (jwtService.isTokenValid(token, user.getEmail())) {

                        SimpleGrantedAuthority authority =
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole().name()
                                );

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        List.of(authority)
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);
                                System.out.println(
        "JWT FILTER -> AUTHENTICATED USER: "
                + user.getEmail()
);
                    }
                }
            }

        } catch (JwtException | IllegalArgumentException exception) {

            /*
             * Invalid/expired JWT.
             *
             * We don't expose JWT parsing details to the client.
             * The request continues without authentication and
             * Spring Security will return 401 if the endpoint is protected.
             */
        }
System.out.println(
        "JWT FILTER -> FINAL AUTH: "
                + SecurityContextHolder
                        .getContext()
                        .getAuthentication()
);
        filterChain.doFilter(request, response);
    }
}