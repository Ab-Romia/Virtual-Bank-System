package com.virtualbank.user.web.dto;

public record LoginResponse(
        String userId,
        String username,
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
