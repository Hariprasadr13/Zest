package com.example.productapi.controller;

import com.example.productapi.dto.common.PageResponse;
import com.example.productapi.dto.item.*;
import com.example.productapi.dto.product.*;
import com.example.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
    private final ProductService service;

    @GetMapping
    @Operation(summary = "List products with pagination")
    public PageResponse<ProductResponse> findAll(
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        if (!java.util.Set.of("id", "productName", "createdBy", "createdOn", "modifiedBy", "modifiedOn").contains(sortBy)) {
            throw new com.example.productapi.exception.BadRequestException("Unsupported sort field: " + sortBy);
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new com.example.productapi.exception.BadRequestException("Sort direction must be 'asc' or 'desc'");
        }

        var result = service.findAll(page, size, sortBy, direction);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request, Authentication auth) {
        ProductResponse response = service.create(request, auth.getName());
        service.auditProductMutation("CREATE", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request, Authentication auth) {
        ProductResponse response = service.update(id, request, auth.getName());
        service.auditProductMutation("UPDATE", id);
        return response;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
        service.auditProductMutation("DELETE", id);
    }

    @GetMapping("/{id}/items")
    public List<ItemResponse> items(@PathVariable Long id) {
        return service.findItems(id);
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ItemResponse> addItem(@PathVariable Long id, @Valid @RequestBody ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(id, request.quantity()));
    }
}
