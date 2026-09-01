package com.example.productapi.service;

import com.example.productapi.dto.product.*;
import com.example.productapi.entity.Product;
import com.example.productapi.repository.*;
import com.example.productapi.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProductServiceTest {
    @Mock
    ProductRepository productRepository;
    @Mock
    ItemRepository itemRepository;
    @InjectMocks
    ProductService service;

    @Test
    void create_setsAuditFields() {
        Product saved = Product.builder().id(1L).productName("Phone").createdBy("hariprasad").createdOn(Instant.now()).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        var result = service.create(new ProductCreateRequest(" Phone "), "hariprasad");
        assertEquals(1L, result.id());
        assertEquals("Phone", result.productName());
        assertEquals("hariprasad", result.createdBy());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
