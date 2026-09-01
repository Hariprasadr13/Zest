package com.example.productapi.dto.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCreateRequest(@NotNull @Positive Integer quantity) {
}
