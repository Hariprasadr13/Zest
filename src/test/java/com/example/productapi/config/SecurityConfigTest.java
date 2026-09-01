package com.example.productapi.config;

import com.example.productapi.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void passwordEncoder_encodesAndMatches() {
        var config = new SecurityConfig(mock(JwtAuthenticationFilter.class), mock(AuthenticationEntryPoint.class), mock(AccessDeniedHandler.class));
        PasswordEncoder encoder = config.passwordEncoder();
        String hash = encoder.encode("Password@123");
        assertNotEquals("Password@123", hash);
        assertTrue(encoder.matches("Password@123", hash));
    }

    @Test
    void corsConfiguration_containsExpectedSettings() {
        var config = new SecurityConfig(mock(JwtAuthenticationFilter.class), mock(AuthenticationEntryPoint.class), mock(AccessDeniedHandler.class));
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:3000, http://example.com");
        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(cors);
        assertEquals(2, cors.getAllowedOrigins().size());
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowCredentials());
    }
}
