package com.example.productapi.service.implementation;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.entity.Item;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.repository.ItemRepository;
import com.example.productapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void findById_whenItemExists_returnsItemResponse() {

        Product product = Product.builder()
                .id(5L)
                .build();

        Item item = Item.builder()
                .id(10L)
                .product(product)
                .quantity(2)
                .build();

        when(itemRepository.findById(10L))
                .thenReturn(Optional.of(item));

        ItemResponse result = itemService.findById(10L);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals(5L, result.productId());
        assertEquals(2, result.quantity());

        verify(itemRepository).findById(10L);
    }

    @Test
    void findById_whenItemDoesNotExist_throwsResourceNotFoundException() {

        when(itemRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.findById(999L)
        );

        assertEquals(
                "Item not found with id: 999",
                exception.getMessage()
        );

        verify(itemRepository).findById(999L);
    }

    @Test
    void findAll_returnsPaginatedItems() {

        Product product = Product.builder()
                .id(5L)
                .build();

        Item item1 = Item.builder()
                .id(10L)
                .product(product)
                .quantity(2)
                .build();

        Item item2 = Item.builder()
                .id(11L)
                .product(product)
                .quantity(5)
                .build();

        Page<Item> itemPage = new PageImpl<>(
                List.of(item1, item2),
                PageRequest.of(0, 10),
                2
        );

        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(itemPage);

        DefaultPaginationRequest request =
                new DefaultPaginationRequest(
                        0,
                        10,
                        "id",
                        "ASC"
                );

        Page<ItemResponse> result = itemService.findAll(request);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        assertEquals(
                new ItemResponse(10L, 5L, 2),
                result.getContent().get(0)
        );

        assertEquals(
                new ItemResponse(11L, 5L, 5),
                result.getContent().get(1)
        );

        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAll_whenNoItems_returnsEmptyPage() {

        Page<Item> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        DefaultPaginationRequest request =
                new DefaultPaginationRequest(
                        0,
                        10,
                        "id",
                        "ASC"
                );

        Page<ItemResponse> result = itemService.findAll(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void addItem_createsAndReturnsItem() {

        Product product = Product.builder()
                .id(6L)
                .build();

        Item savedItem = Item.builder()
                .id(20L)
                .product(product)
                .quantity(7)
                .build();

        when(productService.getProduct(6L))
                .thenReturn(product);

        when(itemRepository.save(any(Item.class)))
                .thenReturn(savedItem);

        ItemResponse result = itemService.addItem(6L, 7);

        assertNotNull(result);
        assertEquals(20L, result.id());
        assertEquals(6L, result.productId());
        assertEquals(7, result.quantity());

        verify(productService).getProduct(6L);

        verify(itemRepository).save(
                argThat(item ->
                        item.getProduct() == product
                                && item.getQuantity() == 7
                )
        );
    }

    @Test
    void addItem_whenProductDoesNotExist_propagatesException() {

        when(productService.getProduct(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Product not found: 999"
                        )
                );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.addItem(999L, 5)
        );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productService).getProduct(999L);

        verifyNoInteractions(itemRepository);
    }

    @Test
    void findItemsByProductId_whenProductExists_returnsItems() {

        Product product = Product.builder()
                .id(5L)
                .build();

        Item item1 = Item.builder()
                .id(10L)
                .product(product)
                .quantity(2)
                .build();

        Item item2 = Item.builder()
                .id(11L)
                .product(product)
                .quantity(5)
                .build();

        when(productService.getProduct(5L))
                .thenReturn(product);

        when(itemRepository.findByProductIdOrderByIdAsc(5L))
                .thenReturn(List.of(item1, item2));

        List<ItemResponse> result =
                itemService.findItemsByProductId(5L);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(
                new ItemResponse(10L, 5L, 2),
                result.get(0)
        );

        assertEquals(
                new ItemResponse(11L, 5L, 5),
                result.get(1)
        );

        verify(productService).getProduct(5L);

        verify(itemRepository)
                .findByProductIdOrderByIdAsc(5L);
    }

    @Test
    void findItemsByProductId_whenNoItems_returnsEmptyList() {

        Product product = Product.builder()
                .id(5L)
                .build();

        when(productService.getProduct(5L))
                .thenReturn(product);

        when(itemRepository.findByProductIdOrderByIdAsc(5L))
                .thenReturn(List.of());

        List<ItemResponse> result =
                itemService.findItemsByProductId(5L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productService).getProduct(5L);

        verify(itemRepository)
                .findByProductIdOrderByIdAsc(5L);
    }

    @Test
    void findItemsByProductId_whenProductDoesNotExist_propagatesException() {

        when(productService.getProduct(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Product not found: 999"
                        )
                );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.findItemsByProductId(999L)
        );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productService).getProduct(999L);

        verifyNoInteractions(itemRepository);
    }
}