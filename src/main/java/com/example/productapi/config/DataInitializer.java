package com.example.productapi.config;

import com.example.productapi.entity.*;
import com.example.productapi.entity.Role;
import com.example.productapi.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedAdmin(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByUsername("admin")) {
                repo.save(AppUser
                        .builder()
                        .username("admin")
                        .password(encoder.encode("Admin@12345"))
                        .role(Role.ADMIN)
                        .build()
                );
            }
        };
    }
}
