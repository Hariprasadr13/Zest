package com.example.productapi.controller;

import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.security.JwtService;
import com.example.productapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void findAll_returnsStablePaginationDto() throws Exception {

        ProductResponse response = new ProductResponse(
                1L,
                "Phone",
                "admin",
                Instant.now(),
                null,
                null
        );

        when(productService.findAll(
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 20),
                        1
                )
        );

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "20")
                                .param("sortBy", "id")
                                .param("direction", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Phone"))
                .andExpect(jsonPath("$.content[0].createdBy").value("admin"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(productService).findAll(0, 20, "id", "asc");
    }


    @Test
    void findAll_usesDefaultValues() throws Exception {

        when(productService.findAll(
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 20),
                        0
                )
        );

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(productService)
                .findAll(0, 20, "id", "asc");
    }


    @Test
    void findAll_supportsDescDirection() throws Exception {

        when(productService.findAll(
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                )
        );

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "productName")
                                .param("direction", "desc")
                )
                .andExpect(status().isOk());

        verify(productService)
                .findAll(0, 10, "productName", "desc");
    }


    @Test
    void findAll_rejectsUnsupportedSortField() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("sortBy", "password")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    void findAll_rejectsUnsupportedDirection() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("direction", "sideways")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    void findAll_rejectsNegativePage() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    void findAll_rejectsZeroSize() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("size", "0")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    void findAll_rejectsSizeAbove100() throws Exception {

        mvc.perform(
                        get("/api/v1/products")
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void findById_returnsProduct() throws Exception {

        ProductResponse response = new ProductResponse(
                1L,
                "Phone",
                "admin",
                Instant.now(),
                null,
                null
        );

        when(productService.findById(1L))
                .thenReturn(response);

        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Phone"))
                .andExpect(jsonPath("$.createdBy").value("admin"));

        verify(productService).findById(1L);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_returns201AndAudits() throws Exception {

        ProductResponse response = new ProductResponse(
                1L,
                "Laptop",
                "user",
                Instant.now(),
                null,
                null
        );

        when(productService.create(any(), eq("user")))
                .thenReturn(response);

        doReturn(CompletableFuture.completedFuture(null))
                .when(productService)
                .auditProductMutation(eq("CREATE"), eq(1L));

        mvc.perform(
                        post("/api/v1/products")
                                .principal(new UsernamePasswordAuthenticationToken(
                                        "user", "password",
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))))
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
                .andExpect(jsonPath("$.createdBy").value("user"));

        verify(productService).create(any(ProductCreateRequest.class), eq("user"));
        verify(productService).auditProductMutation("CREATE", 1L);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_withBlankProductName_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productName\":\"\"}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_withMissingProductName_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void update_returnsProductAndAudits() throws Exception {

        ProductResponse response = new ProductResponse(
                2L,
                "New",
                "user",
                Instant.now(),
                "user",
                Instant.now()
        );

        when(productService.update(
                eq(2L),
                any(ProductUpdateRequest.class),
                eq("user")
        )).thenReturn(response);

        mvc.perform(
                        put("/api/v1/products/2")
                                .principal(new UsernamePasswordAuthenticationToken(
                                        "user", "password",
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productName\":\"New\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.productName").value("New"))
                .andExpect(jsonPath("$.createdBy").value("user"))
                .andExpect(jsonPath("$.modifiedBy").value("user"));

        verify(productService).update(
                eq(2L),
                any(ProductUpdateRequest.class),
                eq("user")
        );

        verify(productService)
                .auditProductMutation("UPDATE", 2L);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void update_withBlankProductName_returns400() throws Exception {

        mvc.perform(
                        put("/api/v1/products/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productName\":\"\"}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void update_withMissingProductName_returns400() throws Exception {

        mvc.perform(
                        put("/api/v1/products/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_returns204AndAudits() throws Exception {

        mvc.perform(delete("/api/v1/products/2"))
                .andExpect(status().isNoContent());

        verify(productService)
                .delete(2L);

        verify(productService)
                .auditProductMutation("DELETE", 2L);
    }

    @Test
    void items_returnsItemList() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                2L,
                3
        );

        when(productService.findItems(2L))
                .thenReturn(List.of(item));

        mvc.perform(get("/api/v1/products/2/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].productId").value(2))
                .andExpect(jsonPath("$[0].quantity").value(3));

        verify(productService)
                .findItems(2L);
    }


    @Test
    void items_returnsEmptyList() throws Exception {

        when(productService.findItems(2L))
                .thenReturn(List.of());

        mvc.perform(get("/api/v1/products/2/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService)
                .findItems(2L);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_returns201() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                2L,
                3
        );

        when(productService.addItem(2L, 3))
                .thenReturn(item);

        mvc.perform(
                        post("/api/v1/products/2/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":3}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(productService)
                .addItem(2L, 3);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_withZeroQuantity_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products/2/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":0}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_withNegativeQuantity_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products/2/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":-1}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addItem_withMissingQuantity_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products/2/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }
}