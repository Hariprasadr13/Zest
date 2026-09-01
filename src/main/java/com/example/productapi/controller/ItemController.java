package com.example.productapi.controller;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.common.PageResponse;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
@Tag(
        name = "Items",
        description = "APIs for managing product items"
)
@SecurityRequirement(name = "bearerAuth")
public class ItemController {

    private final ItemService itemService;

    @Operation(
            summary = "Get item by ID",
            description = "Returns an item using its unique identifier."
    )
    @GetMapping("/{itemId}")
    public ItemResponse getById(@PathVariable Long itemId) {
        return itemService.findById(itemId);
    }

    @Operation(
            summary = "List items",
            description = "Returns a paginated list of items."
    )
    @GetMapping
    public PageResponse<ItemResponse> getAll(@Valid @ModelAttribute DefaultPaginationRequest paginationRequest) {
        Page<ItemResponse> page = itemService.findAll(paginationRequest);
        return PageResponse.from(page);
    }
}