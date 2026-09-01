package com.example.productapi.service;

import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.dto.product.*;
import com.example.productapi.entity.Item;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.repository.ItemRepository;
import com.example.productapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void findAll_normalizesPageAndSize_andMapsResponse() {
        Instant now = Instant.now();
        Product p = Product.builder().id(1L).productName("Phone").createdBy("admin").createdOn(now).build();
        when(productRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(p)));

        var result = service.findAll(-2, 999, "productName", "DESC");

        assertEquals(1, result.getTotalElements());
        assertEquals("Phone", result.getContent().get(0).productName());
        var captor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC,
                captor.getValue().getSort().getOrderFor("productName").getDirection());
    }

    @Test
    void findAll_usesAscendingForNonDescDirection() {
        when(productRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        service.findAll(1, 0, "id", "asc");
        var captor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(1, captor.getValue().getPageSize());
        assertEquals(org.springframework.data.domain.Sort.Direction.ASC,
                captor.getValue().getSort().getOrderFor("id").getDirection());
    }

    @Test
    void findById_returnsMappedProduct() {
        Product p = Product.builder().id(2L).productName("Laptop").createdBy("u")
                .createdOn(Instant.parse("2026-01-01T00:00:00Z"))
                .modifiedBy("admin").modifiedOn(Instant.parse("2026-01-02T00:00:00Z")).build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(p));
        var result = service.findById(2L);
        assertEquals(new ProductResponse(2L, "Laptop", "u", p.getCreatedOn(), "admin", p.getModifiedOn()), result);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void create_trimsNameAndSetsAuditFields() {
        Product saved = Product.builder().id(1L).productName("Phone").createdBy("testUser")
                .createdOn(Instant.now()).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        var result = service.create(new ProductCreateRequest(" Phone "), "testUser");
        assertEquals("Phone", result.productName());
        assertEquals("testUser", result.createdBy());
        assertNotNull(result.createdOn());
        verify(productRepository).save(argThat(p ->
                p.getProductName().equals("Phone") && p.getCreatedBy().equals("testUser") && p.getCreatedOn() != null));
    }

    @Test
    void update_changesNameAndModificationAudit() {
        Product existing = Product.builder().id(3L).productName("Old").createdBy("creator")
                .createdOn(Instant.now()).build();
        when(productRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        var result = service.update(3L, new ProductUpdateRequest(" New Name "), "editor");
        assertEquals("New Name", existing.getProductName());
        assertEquals("editor", existing.getModifiedBy());
        assertNotNull(existing.getModifiedOn());
        assertEquals("New Name", result.productName());
        verify(productRepository).save(existing);
    }

    @Test
    void delete_deletesExistingProduct() {
        Product p = Product.builder().id(4L).productName("Phone").createdBy("u").createdOn(Instant.now()).build();
        when(productRepository.findById(4L)).thenReturn(Optional.of(p));
        service.delete(4L);
        verify(productRepository).delete(p);
    }

    @Test
    void findItems_validatesProductAndMapsItems() {
        Product p = Product.builder().id(5L).build();
        Item i1 = Item.builder().id(10L).product(p).quantity(2).build();
        Item i2 = Item.builder().id(11L).product(p).quantity(5).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(itemRepository.findByProductIdOrderByIdAsc(5L)).thenReturn(List.of(i1, i2));
        List<ItemResponse> result = service.findItems(5L);
        assertEquals(List.of(new ItemResponse(10L, 5L, 2), new ItemResponse(11L, 5L, 5)), result);
    }

    @Test
    void addItem_createsAndReturnsItem() {
        Product p = Product.builder().id(6L).build();
        Item saved = Item.builder().id(20L).product(p).quantity(7).build();
        when(productRepository.findById(6L)).thenReturn(Optional.of(p));
        when(itemRepository.save(any(Item.class))).thenReturn(saved);
        var result = service.addItem(6L, 7);
        assertEquals(new ItemResponse(20L, 6L, 7), result);
        verify(itemRepository).save(argThat(i -> i.getProduct() == p && i.getQuantity() == 7));
    }

    @Test
    void auditProductMutation_completesFuture() throws Exception {
        assertNull(service.auditProductMutation("CREATE", 1L).get(1, TimeUnit.SECONDS));
    }
}
