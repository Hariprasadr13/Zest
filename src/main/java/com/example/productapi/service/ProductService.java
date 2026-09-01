package com.example.productapi.service;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.entity.Product;
import org.springframework.data.domain.Page;

public interface ProductService {

    Page<ProductResponse> findAll(DefaultPaginationRequest paginationRequest);

    ProductResponse findById(Long id);

    ProductResponse create(ProductCreateRequest request, String username);

    ProductResponse update(Long id, ProductUpdateRequest request, String username);

    void delete(Long id);

    Product getProduct(Long id);
}
