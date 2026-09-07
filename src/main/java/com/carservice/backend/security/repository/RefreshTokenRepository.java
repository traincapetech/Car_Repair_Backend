package com.carservice.backend.security.repository;

import com.carservice.backend.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @Query("""
        SELECT rt
        FROM RefreshToken rt
        JOIN FETCH rt.user
        WHERE rt.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> findByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revoked = true
        WHERE rt.user.id = :userId AND rt.revoked = false
    """)
    void revokeAllByUserId(@Param("userId") Long userId);
}