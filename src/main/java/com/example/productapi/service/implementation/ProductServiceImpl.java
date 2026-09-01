package com.example.productapi.service.implementation;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.pagination.PaginationFields;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.ProductService;
import com.example.productapi.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Page<ProductResponse> findAll(DefaultPaginationRequest paginationRequest) {
        Pageable pageable = PaginationUtils.createPageable(paginationRequest, PaginationFields.PRODUCT);
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request, String username) {
        Instant now = Instant.now();

        Product product = Product.builder()
                .productName(request.productName().trim())
                .createdBy(username)
                .createdOn(now)
                .build();

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request, String username) {
        Product product = getProduct(id);

        product.setProductName(request.productName().trim());
        product.setModifiedBy(username);
        product.setModifiedOn(Instant.now());

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    @Override
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getCreatedBy(),
                product.getCreatedOn(),
                product.getModifiedBy(),
                product.getModifiedOn()
        );
    }

}