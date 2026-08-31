package com.carservice.backend.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
