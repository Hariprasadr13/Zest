package com.example.productapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityExceptionHandlerConfigTest {

    private final SecurityExceptionHandlerConfig config = new SecurityExceptionHandlerConfig();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void authenticationEntryPoint_writes401Json() throws Exception {
        var req = new MockHttpServletRequest("GET", "/secure");
        var res = new MockHttpServletResponse();
        config.authenticationEntryPoint(mapper).commence(req, res, new BadCredentialsException("bad"));
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentType().contains("application/json"));
        String content = res.getContentAsString();
        assertTrue(content.contains("Authentication is required"));
        assertTrue(content.contains("\"path\":\"/secure\""));
    }

    @Test
    void accessDeniedHandler_writes403Json() throws Exception {
        var req = new MockHttpServletRequest("GET", "/admin");
        var res = new MockHttpServletResponse();
        config.accessDeniedHandler(mapper).handle(req, res, new AccessDeniedException("no"));
        assertEquals(403, res.getStatus());
        assertTrue(res.getContentType().contains("application/json"));
        String content = res.getContentAsString();
        assertTrue(content.contains("Forbidden"));
        assertTrue(content.contains("You do not have permission"));
        assertTrue(content.contains("\"path\":\"/admin\""));
    }
}