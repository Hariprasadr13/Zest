package com.example.productapi;

import com.example.productapi.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductApiIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Test
    void healthlessSmoke_registerAndLogin() {
        var register = rest.postForEntity("/api/v1/auth/register", new RegisterRequest("integration_user", "Password@123"), Object.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());
        var login = rest.postForEntity("/api/v1/auth/login", new com.example.productapi.dto.auth.LoginRequest("integration_user", "Password@123"), com.example.productapi.dto.auth.AuthResponse.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertNotNull(login.getBody());
        assertNotNull(login.getBody().accessToken());
        assertNotNull(login.getBody().refreshToken());
    }
}
