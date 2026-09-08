package com.carservice.backend.security.jwt;

import com.carservice.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;


    // Generate Access Token
    public String generateAccessToken(User user) {

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + accessTokenExpiration
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }


    // Generate Refresh Token
    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + refreshTokenExpiration
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }


    // Extract email
    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }


    // Extract token type
    public String extractTokenType(String token) {

        return extractAllClaims(token)
                .get(CLAIM_TOKEN_TYPE, String.class);
    }

    // Check if token is explicitly an ACCESS token
    public boolean isAccessToken(String token) {
        try {
            return TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
        } catch (Exception exception) {
            return false;
        }
    }

    // Check if token is explicitly a REFRESH token
    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
        } catch (Exception exception) {
            return false;
        }
    }


    // Validate access token
    public boolean isTokenValid(
            String token,
            String email
    ) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);

            return TOKEN_TYPE_ACCESS.equals(tokenType)
                    && email != null
                    && email.equals(claims.getSubject())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }


    // Check expiration
    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token)
                    .getExpiration()
                    .before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException exception) {
            return true;
        } catch (Exception exception) {
            return true;
        }
    }


    // Extract all claims
    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // Create signing key
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public LocalDateTime getRefreshTokenExpiryDate() {

        return LocalDateTime.now()
                .plus(refreshTokenExpiration, java.time.temporal.ChronoUnit.MILLIS);
    }
}