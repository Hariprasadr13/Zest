package com.example.productapi.dto.auth;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
}
