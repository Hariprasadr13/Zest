package com.example.productapi.security;

import com.example.productapi.entity.AppUser;
import com.example.productapi.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @Mock
    JwtService jwtService;
    @Mock
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @Mock
    FilterChain chain;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void bearerValidToken_setsAuthentication() throws Exception {
        AppUser user = AppUser.builder().username("user").password("p").role(Role.USER).build();
        when(jwtService.extractUsername("token")).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenReturn(user);
        when(jwtService.isValid("token", user)).thenReturn(true);

        filter.doFilter(new MockHttpServletRequest() {{
            addHeader("Authorization", "Bearer token");
        }}, new MockHttpServletResponse(), chain);

        assertNotNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user", org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void noAuthorizationHeader_passesThrough() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void nonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void invalidToken_isIgnoredAndPassesThrough() throws Exception {
        when(jwtService.extractUsername("bad")).thenThrow(new RuntimeException("invalid"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void existingAuthentication_doesNotReplaceIt() throws Exception {
        var existing = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing", null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(existing);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertSame(existing, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }
}
