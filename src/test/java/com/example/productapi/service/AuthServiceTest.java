package com.example.productapi.service;

import com.example.productapi.dto.auth.LoginRequest;
import com.example.productapi.dto.auth.RefreshRequest;
import com.example.productapi.dto.auth.RegisterRequest;
import com.example.productapi.dto.auth.UserResponse;
import com.example.productapi.entity.AppUser;
import com.example.productapi.entity.RefreshToken;
import com.example.productapi.entity.Role;
import com.example.productapi.exception.BadRequestException;
import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.repository.RefreshTokenRepository;
import com.example.productapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {
    @Mock
    AppUserRepository userRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtService jwtService;
    @InjectMocks
    AuthService service;

    @BeforeEach
    void setExpiration() {
        ReflectionTestUtils.setField(service, "refreshExpiration", 604800000L);
    }

    @Test
    void register_createsUserWithUserRole() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded");
        AppUser saved = AppUser.builder().id(1L).username("user").password("encoded").role(Role.USER).build();
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        var result = service.register(new RegisterRequest("user", "Password@123"));

        assertEquals(new UserResponse(1L, "user", "USER"), result);
        verify(userRepository).save(argThat(u -> u.getUsername().equals("user")
                && u.getPassword().equals("encoded") && u.getRole() == Role.USER));
    }

    @Test
    void register_whenDuplicate_throwsBadRequest() {
        when(userRepository.existsByUsername("user")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.register(new RegisterRequest("user", "Password@123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_authenticatesAndIssuesTokens() {
        AppUser user = AppUser.builder().id(2L).username("user").password("encoded").role(Role.USER).build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.login(new LoginRequest("user", "Password@123"));

        assertEquals("access", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900L, result.expiresInSeconds());
        assertNotNull(result.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_rotatesValidToken() {
        AppUser user = AppUser.builder().id(3L).username("user").password("p").role(Role.USER).build();
        String oldToken = "old-refresh-token";
        RefreshToken current = RefreshToken.builder().id(10L).user(user)
                .tokenHash(AuthService.sha256(oldToken))
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(oldToken))).thenReturn(Optional.of(current));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.refresh(new RefreshRequest(oldToken));

        assertEquals("new-access", result.accessToken());
        assertNotNull(current.getRevokedAt());
        assertNotNull(current.getReplacedByHash());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_invalidToken_throwsBadRequest() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.refresh(new RefreshRequest("bad")));
    }

    @Test
    void refresh_revokedToken_throwsBadRequest() {
        RefreshToken token = RefreshToken.builder().user(AppUser.builder().username("u").role(Role.USER).build())
                .expiresAt(Instant.now().plusSeconds(60)).revokedAt(Instant.now()).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> service.refresh(new RefreshRequest("token")));
    }

    @Test
    void refresh_expiredToken_throwsBadRequest() {
        RefreshToken token = RefreshToken.builder().user(AppUser.builder().username("u").role(Role.USER).build())
                .expiresAt(Instant.now().minusSeconds(1)).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> service.refresh(new RefreshRequest("token")));
    }

    @Test
    void sha256_isDeterministicAnd64HexCharacters() {
        String hash = AuthService.sha256("hello");
        assertEquals(64, hash.length());
        assertEquals(hash, AuthService.sha256("hello"));
        assertNotEquals(hash, AuthService.sha256("world"));
    }
}
