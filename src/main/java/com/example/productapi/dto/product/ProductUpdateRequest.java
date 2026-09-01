package com.example.productapi.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(@NotBlank @Size(max = 255) String productName) {
}
