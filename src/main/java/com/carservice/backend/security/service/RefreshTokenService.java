package com.carservice.backend.security.service;

import com.carservice.backend.common.exception.InvalidCredentialsException;
import com.carservice.backend.security.entity.RefreshToken;
import com.carservice.backend.security.repository.RefreshTokenRepository;
import com.carservice.backend.security.util.TokenHashUtil;
import com.carservice.backend.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;
    }

    public void saveRefreshToken(
            String refreshToken,
            User user,
            LocalDateTime expiresAt
    ) {

        String tokenHash =
                TokenHashUtil.hash(refreshToken);

        RefreshToken token =
                new RefreshToken();

        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setExpiresAt(expiresAt);
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }

    public RefreshToken findByToken(String refreshToken) {

        String tokenHash =
                TokenHashUtil.hash(refreshToken);

        return refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid refresh token"
                        )
                );
    }

    public void validateRefreshToken(
            RefreshToken refreshToken
    ) {

        if (refreshToken.getRevoked()) {
            throw new InvalidCredentialsException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidCredentialsException(
                    "Refresh token has expired"
            );
        }
    }

    public void revokeToken(
            RefreshToken refreshToken
    ) {

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    /*
     * Logout
     *
     * Finds the refresh token,
     * validates that it is still active,
     * and then revokes it.
     */
    public void logout(String refreshToken) {

        RefreshToken storedToken =
                findByToken(refreshToken);

        validateRefreshToken(storedToken);

        revokeToken(storedToken);
    }
}