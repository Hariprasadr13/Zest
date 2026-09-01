package com.example.productapi.service.implementation;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.product.ProductCreateRequest;
import com.example.productapi.dto.product.ProductResponse;
import com.example.productapi.dto.product.ProductUpdateRequest;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void findAll_normalizesPageAndSize_andMapsResponse() {

        Instant now = Instant.now();

        Product product = Product.builder()
                .id(1L)
                .productName("Phone")
                .createdBy("admin")
                .createdOn(now)
                .build();

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        DefaultPaginationRequest request =
                new DefaultPaginationRequest(
                        0,
                        100,
                        "productName",
                        "DESC"
                );

        var result = productService.findAll(request);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Phone", result.getContent().get(0).productName());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort()
                        .getOrderFor("productName")
                        .getDirection()
        );
    }

    @Test
    void findAll_usesAscendingDirection() {

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        DefaultPaginationRequest request =
                new DefaultPaginationRequest(
                        1,
                        1,
                        "id",
                        "asc"
                );

        var result = productService.findAll(request);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(1, pageable.getPageSize());
        assertEquals(
                Sort.Direction.ASC,
                pageable.getSort()
                        .getOrderFor("id")
                        .getDirection()
        );
    }

    @Test
    void findById_returnsMappedProduct() {

        Instant createdOn =
                Instant.parse("2026-01-01T00:00:00Z");

        Instant modifiedOn =
                Instant.parse("2026-01-02T00:00:00Z");

        Product product = Product.builder()
                .id(2L)
                .productName("Laptop")
                .createdBy("u")
                .createdOn(createdOn)
                .modifiedBy("admin")
                .modifiedOn(modifiedOn)
                .build();

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product));

        ProductResponse result =
                productService.findById(2L);

        assertEquals(
                new ProductResponse(
                        2L,
                        "Laptop",
                        "u",
                        createdOn,
                        "admin",
                        modifiedOn
                ),
                result
        );

        verify(productRepository).findById(2L);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.findById(99L)
                );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        verify(productRepository).findById(99L);
    }

    @Test
    void create_trimsNameAndSetsAuditFields() {

        Instant savedTime = Instant.now();

        Product savedProduct = Product.builder()
                .id(1L)
                .productName("Phone")
                .createdBy("testUser")
                .createdOn(savedTime)
                .build();

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse result =
                productService.create(
                        new ProductCreateRequest(" Phone "),
                        "testUser"
                );

        assertEquals("Phone", result.productName());
        assertEquals("testUser", result.createdBy());
        assertEquals(savedTime, result.createdOn());

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(captor.capture());

        Product productToSave = captor.getValue();

        assertEquals("Phone", productToSave.getProductName());
        assertEquals("testUser", productToSave.getCreatedBy());
        assertNotNull(productToSave.getCreatedOn());
    }

    @Test
    void update_changesNameAndModificationAudit() {

        Instant createdOn = Instant.now();

        Product existingProduct = Product.builder()
                .id(3L)
                .productName("Old")
                .createdBy("creator")
                .createdOn(createdOn)
                .build();

        when(productRepository.findById(3L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        ProductResponse result =
                productService.update(
                        3L,
                        new ProductUpdateRequest(" New Name "),
                        "editor"
                );

        assertEquals("New Name", existingProduct.getProductName());
        assertEquals("editor", existingProduct.getModifiedBy());
        assertNotNull(existingProduct.getModifiedOn());

        assertEquals("New Name", result.productName());
        assertEquals("creator", result.createdBy());
        assertEquals(createdOn, result.createdOn());

        verify(productRepository).findById(3L);
        verify(productRepository).save(existingProduct);
    }

    @Test
    void update_whenProductMissing_throwsNotFound() {

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.update(
                                99L,
                                new ProductUpdateRequest("Updated"),
                                "editor"
                        )
                );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        verify(productRepository).findById(99L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void delete_deletesExistingProduct() {

        Product product = Product.builder()
                .id(4L)
                .productName("Phone")
                .createdBy("u")
                .createdOn(Instant.now())
                .build();

        when(productRepository.findById(4L))
                .thenReturn(Optional.of(product));

        productService.delete(4L);

        verify(productRepository).findById(4L);
        verify(productRepository).delete(product);
    }

    @Test
    void delete_whenProductMissing_throwsNotFound() {

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.delete(99L)
                );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        verify(productRepository).findById(99L);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getProduct_returnsExistingProduct() {

        Product product = Product.builder()
                .id(10L)
                .productName("Tablet")
                .build();

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        Product result =
                productService.getProduct(10L);

        assertSame(product, result);

        verify(productRepository).findById(10L);
    }

    @Test
    void getProduct_whenMissing_throwsNotFound() {

        when(productRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.getProduct(20L)
                );

        assertEquals(
                "Product not found with id: 20",
                exception.getMessage()
        );

        verify(productRepository).findById(20L);
    }
}