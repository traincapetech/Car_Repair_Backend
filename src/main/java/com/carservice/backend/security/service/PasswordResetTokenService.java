package com.carservice.backend.security.service;

import com.carservice.backend.common.exception.InvalidCredentialsException;
import com.carservice.backend.security.entity.PasswordResetToken;
import com.carservice.backend.security.repository.PasswordResetTokenRepository;
import com.carservice.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;

    @Value("${password-reset.token-expiration}")
    private long tokenExpirationMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(
            PasswordResetTokenRepository tokenRepository
    ) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String createPasswordResetToken(User user) {

        /*
         * Invalidate any previous reset tokens.
         */
        tokenRepository.findAll()
                .stream()
                .filter(token ->
                        token.getUser().getId().equals(user.getId())
                                && !token.getUsed()
                )
                .forEach(token -> token.setUsed(true));

        /*
         * Generate a cryptographically secure random token.
         */
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        /*
         * Store only the SHA-256 hash in the database.
         */
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(tokenExpirationMinutes)
        );
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        /*
         * Return raw token.
         *
         * Development only.
         * Later this will be sent through email.
         */
        return rawToken;
    }

    public PasswordResetToken validateToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new InvalidCredentialsException(
                                        "Invalid or expired reset token"
                                ));

        if (resetToken.getUsed()) {
            throw new InvalidCredentialsException(
                    "Invalid or expired reset token"
            );
        }

        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidCredentialsException(
                    "Invalid or expired reset token"
            );
        }

        return resetToken;
    }

    @Transactional
    public void markTokenAsUsed(
            PasswordResetToken resetToken
    ) {

        resetToken.setUsed(true);

        tokenRepository.save(resetToken);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}