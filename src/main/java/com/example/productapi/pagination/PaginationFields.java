package com.example.productapi.pagination;

import java.util.Set;

public final class PaginationFields {

    private PaginationFields() {
    }

    public static final Set<String> PRODUCT = Set.of(
            "id",
            "productName",
            "createdBy",
            "createdOn",
            "modifiedBy",
            "modifiedOn"
    );

    public static final Set<String> ITEM = Set.of(
            "id",
            "product",
            "quantity"
    );
}