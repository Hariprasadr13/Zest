package com.example.productapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    @Test
    void notFound() {
        assertEquals(404, handler.notFound(new ResourceNotFoundException("missing"), request).getStatusCode().value());
    }

    @Test
    void badRequest() {
        assertEquals(400, handler.badRequest(new BadRequestException("bad"), request).getStatusCode().value());
    }

    @Test
    void badCredentials() {
        var body = handler.badCredentials(new BadCredentialsException("bad"), request).getBody();
        assertNotNull(body);
        assertEquals(401, body.status());
        assertEquals("Invalid username or password", body.message());
    }

    @Test
    void conflict() {
        assertEquals(409, handler.conflict(new DataIntegrityViolationException("duplicate"), request).getStatusCode().value());
    }

    @Test
    void malformedTypeMismatch() {
        var ex = new MethodArgumentTypeMismatchException("x", Long.class, "id", null, new IllegalArgumentException());
        assertEquals(400, handler.malformedRequest(ex, request).getStatusCode().value());
    }

    @Test
    void malformedMissingParameter() {
        var ex = new MissingServletRequestParameterException("id", "Long");
        assertEquals(400, handler.malformedRequest(ex, request).getStatusCode().value());
    }

    @Test
    void constraintViolation() {
        @SuppressWarnings("unchecked") ConstraintViolation<Object> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(new jakarta.validation.Path() {
            @Override
            public String toString() {
                return "quantity";
            }

            @Override
            public java.util.Iterator<Node> iterator() {
                return Collections.emptyIterator();
            }
        });
        when(violation.getMessage()).thenReturn("must be positive");
        Set<ConstraintViolation<?>> violations = Set.of(violation);
        ConstraintViolationException ex = new ConstraintViolationException("validation", violations);
        var body = handler.constraintViolation(ex, request).getBody();
        assertNotNull(body);
        assertEquals("must be positive", body.validationErrors().get("quantity"));
    }

    @Test
    void validation() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "productName", "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, binding);
        var body = handler.validation(ex, request).getBody();
        assertNotNull(body);
        assertEquals("must not be blank", body.validationErrors().get("productName"));
    }

    @Test
    void accessDenied() {
        assertEquals(403, handler.accessDenied(new AccessDeniedException("no"), request).getStatusCode().value());
    }

    @Test
    void generic() {
        assertEquals(500, handler.generic(new RuntimeException("boom"), request).getStatusCode().value());
    }
}