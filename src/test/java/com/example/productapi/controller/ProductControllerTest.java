package com.example.productapi.controller;

import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.security.JwtService;
import com.example.productapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
    void invalidCreate_returns400() throws Exception {

        mvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "productName": ""
                                }
                                """)
                )
                .andExpect(status().isBadRequest());
    }
    @Test
    void findAll_returnsStablePaginationDto() throws Exception {
        var response = new com.example.productapi.dto.product.ProductResponse(
                1L, "Phone", "admin", Instant.now(), null, null);
        when(productService.findAll(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(response), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "id")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void findAll_rejectsUnsupportedSortField() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/products")
                        .param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

}
