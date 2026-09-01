package com.example.productapi.entity;

import com.example.productapi.dto.auth.*;
import com.example.productapi.dto.common.PageResponse;
import com.example.productapi.dto.item.ItemCreateRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAndDtoTest {
    @Test
    void appUser_implementsUserDetails() {
        AppUser user = AppUser.builder().id(1L).username("user").password("p").role(Role.ADMIN).build();
        assertEquals("user", user.getUsername());
        assertEquals("p", user.getPassword());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_ADMIN", user.getAuthorities().iterator().next().getAuthority());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void entities_buildersAndAccessorsWork() {
        Instant now = Instant.now();
        Product p = Product.builder().id(1L).productName("Phone").createdBy("u").createdOn(now).build();
        Item item = Item.builder().id(2L).product(p).quantity(3).build();
        RefreshToken token = RefreshToken.builder().id(3L).tokenHash("hash").user(
                        AppUser.builder().username("u").role(Role.USER).password("p").build())
                .expiresAt(now).build();
        p.setItems(List.of(item));
        token.setRevokedAt(now);
        token.setReplacedByHash("new");
        assertEquals(item, p.getItems().get(0));
        assertEquals("hash", token.getTokenHash());
        assertEquals("new", token.getReplacedByHash());
    }

    @Test
    void recordsExposeValues() {
        Instant now = Instant.now();
        assertEquals("user", new RegisterRequest("user", "Password@123").username());
        assertEquals("Password@123", new LoginRequest("user", "Password@123").password());
        assertEquals("r", new RefreshRequest("r").refreshToken());
        assertEquals("USER", new UserResponse(1L, "user", "USER").role());
        assertEquals("Bearer", new AuthResponse("a", "r", "Bearer", 900).tokenType());
        assertEquals(2, new PageResponse<>(List.of("a", "b"), 0, 2, 2, 1, true, true).content().size());
        assertEquals(5, new ItemCreateRequest(5).quantity());
        assertEquals(1L, new ItemResponse(1L, 2L, 3).id());
        assertEquals("Phone", new ProductCreateRequest("Phone").productName());
        assertEquals("Phone", new ProductUpdateRequest("Phone").productName());
        assertEquals("Phone", new ProductResponse(1L, "Phone", "u", now, null, null).productName());
    }
}
