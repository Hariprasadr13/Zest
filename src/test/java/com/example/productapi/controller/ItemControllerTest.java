package com.example.productapi.controller;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.repository.AppUserRepository;
import com.example.productapi.security.JwtService;
import com.example.productapi.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void getById_returnsItem() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                2L,
                3
        );

        when(itemService.findById(10L))
                .thenReturn(item);

        mvc.perform(
                        get("/api/v1/items/10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(itemService)
                .findById(10L);
    }

    @Test
    void getAll_returnsPaginatedItems() throws Exception {

        ItemResponse item = new ItemResponse(
                10L,
                2L,
                3
        );

        PageImpl<ItemResponse> page = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 10),
                1
        );

        when(itemService.findAll(any(DefaultPaginationRequest.class)))
                .thenReturn(page);

        mvc.perform(
                        get("/api/v1/items")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("sortDirection", "ASC")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].productId").value(2))
                .andExpect(jsonPath("$.content[0].quantity").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(itemService)
                .findAll(any(DefaultPaginationRequest.class));
    }

    @Test
    void getAll_returnsEmptyPage() throws Exception {

        PageImpl<ItemResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(itemService.findAll(any(DefaultPaginationRequest.class)))
                .thenReturn(page);

        mvc.perform(
                        get("/api/v1/items")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("sortDirection", "ASC")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(itemService)
                .findAll(any(DefaultPaginationRequest.class));
    }
}