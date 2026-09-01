package com.example.productapi.service.implementation;

import com.example.productapi.entity.AppUser;
import com.example.productapi.entity.Role;
import com.example.productapi.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    AppUserRepository repository;
    @InjectMocks
    UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_returnsUser() {
        AppUser user = AppUser.builder().username("user").password("p").role(Role.USER).build();
        when(repository.findByUsername("user")).thenReturn(Optional.of(user));
        assertSame(user, service.loadUserByUsername("user"));
    }

    @Test
    void loadUserByUsername_missing_throwsException() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing"));
    }
}
