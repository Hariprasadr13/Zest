package com.example.productapi;

import com.example.productapi.dto.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductApiIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Test
    void registerAndLogin_returnsTokens() {
        String username = "integration_" + System.nanoTime();
        var register = rest.postForEntity("/api/v1/auth/register", new RegisterRequest(username, "Password@123"), UserResponse.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());
        assertNotNull(register.getBody());
        var login = rest.postForEntity("/api/v1/auth/login", new LoginRequest(username, "Password@123"), AuthResponse.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertNotNull(login.getBody());
        assertNotNull(login.getBody().accessToken());
        assertNotNull(login.getBody().refreshToken());
    }

    @Test
    void protectedProductEndpoint_withoutToken_returns401() {
        var response = rest.getForEntity("/api/v1/products", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Authentication is required"));
    }

    @Test
    void invalidCredentials_returns401() {
        String username = "bad_login_" + System.nanoTime();
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, "Password@123"), UserResponse.class);

        var response = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(username, "WrongPassword@123"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void authenticatedUser_canCreateAndReadProduct() {
        String username = "crud_" + System.nanoTime();
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, "Password@123"), UserResponse.class);
        var login = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(username, "Password@123"), AuthResponse.class);
        assertNotNull(login.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().accessToken());

        var create = rest.exchange("/api/v1/products", HttpMethod.POST,
                new HttpEntity<>(new com.example.productapi.dto.product.ProductCreateRequest("Integration Phone"), headers),
                com.example.productapi.dto.product.ProductResponse.class);
        assertEquals(HttpStatus.CREATED, create.getStatusCode());
        assertNotNull(create.getBody());
        Long id = create.getBody().id();

        var get = rest.exchange("/api/v1/products/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), com.example.productapi.dto.product.ProductResponse.class);
        assertEquals(HttpStatus.OK, get.getStatusCode());
        assertEquals("Integration Phone", get.getBody().productName());
    }

    @Test
    void refreshToken_canBeRotatedAndOldTokenRejected() {
        String username = "refresh_" + System.nanoTime();
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, "Password@123"), UserResponse.class);
        var login = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(username, "Password@123"), AuthResponse.class);
        assertNotNull(login.getBody());

        var refresh = rest.postForEntity("/api/v1/auth/refresh",
                new RefreshRequest(login.getBody().refreshToken()), AuthResponse.class);
        assertEquals(HttpStatus.OK, refresh.getStatusCode());
        assertNotNull(refresh.getBody());
        assertNotEquals(login.getBody().refreshToken(), refresh.getBody().refreshToken());

        var reused = rest.postForEntity("/api/v1/auth/refresh",
                new RefreshRequest(login.getBody().refreshToken()), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, reused.getStatusCode());
    }
}
