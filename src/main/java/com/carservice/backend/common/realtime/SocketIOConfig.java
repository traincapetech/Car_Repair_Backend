package com.carservice.backend.common.realtime;

import com.carservice.backend.security.jwt.JwtService;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@org.springframework.context.annotation.Configuration
public class SocketIOConfig {

    private static final Logger log = LoggerFactory.getLogger(SocketIOConfig.class);

    @Value("${socketio.host:0.0.0.0}")
    private String host;

    @Value("${socketio.port:9092}")
    private int port;

    private final JwtService jwtService;

    public SocketIOConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin(null); // Allow all configured CORS origins

        config.setAuthorizationListener(data -> {
            String token = null;

            // 1. Try URL query param ?token=...
            if (data.getSingleUrlParam("token") != null) {
                token = data.getSingleUrlParam("token");
            }

            // 2. Try Authorization header
            if (token == null && data.getHttpHeaders().get("Authorization") != null) {
                String authHeader = data.getHttpHeaders().get("Authorization");
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else {
                    token = authHeader;
                }
            }

            if (token == null || token.isBlank()) {
                log.warn("[SocketIO] Rejected connection without JWT token from {}", data.getAddress());
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }

            try {
                if (!jwtService.isAccessToken(token) || jwtService.isTokenExpired(token)) {
                    log.warn("[SocketIO] Rejected invalid or expired token from {}", data.getAddress());
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }

                Claims claims = jwtService.extractAllClaims(token);
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);
                String email = claims.getSubject();

                if (userId == null || role == null) {
                    log.warn("[SocketIO] Rejected token missing userId or role from {}", data.getAddress());
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }

                Map<String, Object> storeParams = new HashMap<>();
                storeParams.put("userId", userId);
                storeParams.put("role", role);
                storeParams.put("email", email);

                log.info("[SocketIO] Authorized client handshake: userId={}, role={}, email={}", userId, role, email);
                return new AuthorizationResult(true, storeParams);
            } catch (Exception ex) {
                log.error("[SocketIO] Token verification error during handshake: {}", ex.getMessage());
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        return new SocketIOServer(config);
    }
}
