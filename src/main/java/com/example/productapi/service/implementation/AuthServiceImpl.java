package com.example.productapi.service.implementation;

import com.example.productapi.dto.auth.*;
import com.example.productapi.entity.AppUser;
import com.example.productapi.entity.RefreshToken;
import com.example.productapi.entity.Role;
import com.example.productapi.exception.BadRequestException;
import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.repository.RefreshTokenRepository;
import com.example.productapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshExpiration;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new BadRequestException("Username already exists");
        AppUser user = userRepository.save(AppUser.builder().username(request.username()).password(passwordEncoder.encode(request.password())).role(Role.USER).build());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUser user = userRepository.findByUsername(request.username()).orElseThrow();
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (current.getRevokedAt() != null || current.getExpiresAt().isBefore(Instant.now()))
            throw new BadRequestException("Refresh token is expired or revoked");

        current.setRevokedAt(Instant.now());
        AuthResponse response = issueTokens(current.getUser());
        current.setReplacedByHash(sha256(response.refreshToken()));
        refreshTokenRepository.save(current);
        return response;
    }

    private AuthResponse issueTokens(AppUser user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = randomToken();
        refreshTokenRepository.save(RefreshToken.builder().tokenHash(sha256(refresh)).user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiration)).build());
        return new AuthResponse(access, refresh, "Bearer", jwtService.getAccessExpirationSeconds());
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
