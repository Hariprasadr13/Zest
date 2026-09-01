package com.example.productapi.service;

import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.*;
import com.example.productapi.entity.*;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(int page, int size, String sortBy, String direction) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return productRepository.findAll(PageRequest.of(Math.max(page, 0), safeSize, sort)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request, String username) {
        Instant now = Instant.now();
        Product product = Product.builder().productName(request.productName().trim()).createdBy(username).createdOn(now).build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request, String username) {
        Product product = getProduct(id);
        product.setProductName(request.productName().trim());
        product.setModifiedBy(username);
        product.setModifiedOn(Instant.now());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findItems(Long productId) {
        getProduct(productId);
        return itemRepository.findByProductIdOrderByIdAsc(productId).stream().map(i -> new ItemResponse(i.getId(), productId, i.getQuantity())).toList();
    }

    @Transactional
    public ItemResponse addItem(Long productId, Integer quantity) {
        Product p = getProduct(productId);
        Item i = itemRepository.save(Item.builder().product(p).quantity(quantity).build());
        return new ItemResponse(i.getId(), productId, i.getQuantity());
    }

    @Async
    public CompletableFuture<Void> auditProductMutation(String action, Long productId) {
        System.out.printf("ASYNC AUDIT action=%s productId=%d%n", action, productId);
        return CompletableFuture.completedFuture(null);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getProductName(), p.getCreatedBy(), p.getCreatedOn(), p.getModifiedBy(), p.getModifiedOn());
    }
}
