package com.example.productapi.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DefaultPaginationRequest(
        @Min(
                value = 0,
                message = "Page must be greater than or equal to 0"
        )
        Integer page,

        @Min(
                value = 1,
                message = "Size must be at least 1"
        )
        @Max(
                value = 100,
                message = "Size must not exceed 100"
        )
        Integer size,

        String sortBy,

        String direction

) {
    public DefaultPaginationRequest {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        direction = direction == null || direction.isBlank() ? "asc" : direction.toLowerCase();
    }
}