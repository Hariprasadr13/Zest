package com.example.productapi.security;

import com.example.productapi.entity.AppUser;
import com.example.productapi.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private JwtService service;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        service = new JwtService("test-secret-test-secret-test-secret-123456789", 900000L);
        user = AppUser.builder().username("user").password("p").role(Role.USER).build();
    }

    @Test
    void generateAndExtractToken() {
        String token = service.generateAccessToken(user);
        assertEquals("user", service.extractUsername(token));
        assertTrue(service.isValid(token, user));
        assertEquals(900L, service.getAccessExpirationSeconds());
    }

    @Test
    void isValid_returnsFalseForDifferentUser() {
        String token = service.generateAccessToken(user);
        UserDetails other = AppUser.builder().username("other").password("p").role(Role.USER).build();
        assertFalse(service.isValid(token, other));
    }

    @Test
    void isValid_returnsFalseForMalformedToken() {
        assertFalse(service.isValid("not-a-jwt", user));
    }

    @Test
    void extractUsername_rejectsMalformedToken() {
        assertThrows(RuntimeException.class, () -> service.extractUsername("not-a-jwt"));
    }
}
