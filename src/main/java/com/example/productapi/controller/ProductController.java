package com.example.productapi.controller;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.common.PageResponse;
import com.example.productapi.dto.item.ItemCreateRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.service.ItemService;
import com.example.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(
        name = "Products",
        description = "APIs for managing products"
)
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final ItemService itemService;

    @Operation(
            summary = "List products",
            description = "Returns a paginated list of products."
    )
    @GetMapping
    public PageResponse<ProductResponse> getAll(@Valid @ModelAttribute DefaultPaginationRequest paginationRequest) {
        Page<ProductResponse> page = productService.findAll(paginationRequest);

        return PageResponse.from(page);
    }

    @Operation(
            summary = "Get product by ID",
            description = "Returns a product using its unique identifier."
    )
    @GetMapping("/{productId}")
    public ProductResponse getById(@PathVariable Long productId) {
        return productService.findById(productId);
    }

    @Operation(
            summary = "Create product",
            description = "Creates a new product."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request, Authentication authentication) {
        ProductResponse response = productService.create(request, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Update product",
            description = "Updates an existing product."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{productId}")
    public ProductResponse update(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request, Authentication authentication) {
        return productService.update(productId, request, authentication.getName());
    }

    @Operation(
            summary = "Delete product",
            description = "Deletes a product by its unique identifier."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId) {
        productService.delete(productId);
    }

    @Operation(
            summary = "Add item to product",
            description = "Creates an item associated with the specified product."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{productId}/items")
    public ResponseEntity<ItemResponse> addItem(@PathVariable Long productId, @Valid @RequestBody ItemCreateRequest request) {
        ItemResponse response = itemService.addItem(productId, request.quantity());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Get product items",
            description = "Returns all items associated with the specified product."
    )
    @GetMapping("/{productId}/items")
    public List<ItemResponse> getItems(@PathVariable Long productId) {
        return itemService.findItemsByProductId(productId);
    }
}
