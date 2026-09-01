package com.example.productapi.controller;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.security.JwtService;
import com.example.productapi.service.ItemService;
import com.example.productapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ItemService itemService;

    /*
     * These mocks are needed only if your SecurityConfig/JWT
     * configuration requires them while loading @WebMvcTest.
     */
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getById_returnsProduct() throws Exception {

        ProductResponse product = new ProductResponse(
                1L,
                "Laptop",
                "admin",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                null
        );

        when(productService.findById(1L))
                .thenReturn(product);

        mvc.perform(
                        get("/api/v1/products/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.createdBy").value("admin"))
                .andExpect(jsonPath("$.createdOn")
                        .value("2026-01-01T10:00:00Z"));

        verify(productService)
                .findById(1L);
    }


    @Test
    void getById_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        get("/api/v1/products/1")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getAll_returnsPaginatedProducts() throws Exception {

        ProductResponse product = new ProductResponse(
                1L,
                "Laptop",
                "admin",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                null
        );

        PageImpl<ProductResponse> page = new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 10),
                1
        );

        when(productService.findAll(any(DefaultPaginationRequest.class)))
                .thenReturn(page);

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("sortDirection", "ASC")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.content[0].createdBy").value("admin"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(productService)
                .findAll(any(DefaultPaginationRequest.class));
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getAll_returnsEmptyPage() throws Exception {

        PageImpl<ProductResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(productService.findAll(any(DefaultPaginationRequest.class)))
                .thenReturn(page);

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("sortDirection", "ASC")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(productService)
                .findAll(any(DefaultPaginationRequest.class));
    }


    @Test
    void getAll_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("sortDirection", "ASC")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_returns201() throws Exception {

        ProductResponse product = new ProductResponse(
                1L,
                "Laptop",
                "admin",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                null
        );

        when(productService.create(
                any(ProductCreateRequest.class),
                eq("admin")
        )).thenReturn(product);

        mvc.perform(
                        post("/api/v1/products")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.createdBy").value("admin"));

        verify(productService)
                .create(
                        any(ProductCreateRequest.class),
                        eq("admin")
                );
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_asUser_returns201() throws Exception {

        ProductResponse product = new ProductResponse(
                2L,
                "Phone",
                "user",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                null
        );

        when(productService.create(
                any(ProductCreateRequest.class),
                eq("user")
        )).thenReturn(product);

        mvc.perform(
                        post("/api/v1/products")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Phone"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.productName").value("Phone"))
                .andExpect(jsonPath("$.createdBy").value("user"));

        verify(productService)
                .create(
                        any(ProductCreateRequest.class),
                        eq("user")
                );
    }


    @Test
    void create_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_withoutCsrfToken_returns403() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_returns200() throws Exception {

        ProductResponse product = new ProductResponse(
                1L,
                "Updated Laptop",
                "admin",
                Instant.parse("2026-01-01T10:00:00Z"),
                "admin",
                Instant.parse("2026-01-02T10:00:00Z")
        );

        when(productService.update(
                eq(1L),
                any(ProductUpdateRequest.class),
                eq("admin")
        )).thenReturn(product);

        mvc.perform(
                        put("/api/v1/products/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Updated Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Updated Laptop"))
                .andExpect(jsonPath("$.modifiedBy").value("admin"));

        verify(productService)
                .update(
                        eq(1L),
                        any(ProductUpdateRequest.class),
                        eq("admin")
                );
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void update_asUser_returns200() throws Exception {

        ProductResponse product = new ProductResponse(
                1L,
                "Updated Laptop",
                "user",
                Instant.parse("2026-01-01T10:00:00Z"),
                "user",
                Instant.parse("2026-01-02T10:00:00Z")
        );

        when(productService.update(
                eq(1L),
                any(ProductUpdateRequest.class),
                eq("user")
        )).thenReturn(product);

        mvc.perform(
                        put("/api/v1/products/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Updated Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Updated Laptop"))
                .andExpect(jsonPath("$.modifiedBy").value("user"));

        verify(productService)
                .update(
                        eq(1L),
                        any(ProductUpdateRequest.class),
                        eq("user")
                );
    }


    @Test
    void update_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        put("/api/v1/products/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Updated Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_withoutCsrfToken_returns403() throws Exception {

        mvc.perform(
                        put("/api/v1/products/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": "Updated Laptop"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_returns204() throws Exception {

        mvc.perform(
                        delete("/api/v1/products/1")
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        verify(productService)
                .delete(1L);
    }

    @Test
    void delete_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        delete("/api/v1/products/1")
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_withoutCsrfToken_returns403() throws Exception {

        mvc.perform(
                        delete("/api/v1/products/1")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_returns201() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                1L,
                3
        );

        when(itemService.addItem(1L, 3))
                .thenReturn(item);

        mvc.perform(
                        post("/api/v1/products/1/items")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "quantity": 3
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(itemService)
                .addItem(1L, 3);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void addItem_asAdmin_returns201() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                1L,
                5
        );

        when(itemService.addItem(1L, 5))
                .thenReturn(item);

        mvc.perform(
                        post("/api/v1/products/1/items")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "quantity": 5
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(5));

        verify(itemService)
                .addItem(1L, 5);
    }


    @Test
    void addItem_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        post("/api/v1/products/1/items")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "quantity": 3
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(itemService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_withoutCsrfToken_returns403() throws Exception {

        mvc.perform(
                        post("/api/v1/products/1/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "quantity": 3
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(itemService);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getItems_returnsItems() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                1L,
                3
        );

        when(itemService.findItemsByProductId(1L))
                .thenReturn(List.of(item));

        mvc.perform(
                        get("/api/v1/products/1/items")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].quantity").value(3));

        verify(itemService)
                .findItemsByProductId(1L);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getItems_returnsEmptyList() throws Exception {

        when(itemService.findItemsByProductId(1L))
                .thenReturn(List.of());

        mvc.perform(
                        get("/api/v1/products/1/items")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemService)
                .findItemsByProductId(1L);
    }


    @Test
    void getItems_withoutAuthentication_returns401() throws Exception {

        mvc.perform(
                        get("/api/v1/products/1/items")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(itemService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_withInvalidRequest_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_withInvalidRequest_returns400() throws Exception {

        mvc.perform(
                        put("/api/v1/products/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productName": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_withInvalidRequest_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products/1/items")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "quantity": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemService);
    }
}