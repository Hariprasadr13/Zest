package com.example.productapi.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(@NotBlank @Size(max = 255) String productName) {
}
