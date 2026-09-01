package com.example.productapi.util;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationUtils {

    public static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

    private PaginationUtils() {
    }

    public static Pageable createPageable(DefaultPaginationRequest request, Set<String> allowedSortFields) {

        if (!allowedSortFields.contains(request.sortBy())) {
            throw new BadRequestException("Unsupported sort field: " + request.sortBy());
        }

        if (!ALLOWED_DIRECTIONS.contains(request.direction())) {
            throw new BadRequestException("Sort direction must be 'asc' or 'desc'");
        }

        return PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.fromString(request.direction()), request.sortBy()));
    }
}