package com.example.productapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.Instant;
import java.util.Map;

@Configuration
public class SecurityExceptionHandlerConfig {
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (req, res, ex) -> write(mapper, res, 401, "Unauthorized", "Authentication is required", req.getRequestURI());
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (req, res, ex) -> write(mapper, res, 403, "Forbidden", "You do not have permission to access this resource", req.getRequestURI());
    }

    private void write(ObjectMapper mapper, HttpServletResponse res, int status, String error, String message, String path) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), Map.of("timestamp", Instant.now(), "status", status, "error", error, "message", message, "path", path, "validationErrors", Map.of()));
    }
}
