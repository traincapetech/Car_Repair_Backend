package com.carservice.backend.user.dto;

public record TokenResponse(

        String accessToken,
        String refreshToken

) {
}
