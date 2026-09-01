package com.example.productapi.service.implementation;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.item.ItemResponse;
import com.example.productapi.entity.Item;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.pagination.PaginationFields;
import com.example.productapi.repository.ItemRepository;
import com.example.productapi.service.ItemService;
import com.example.productapi.service.ProductService;
import com.example.productapi.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ProductService productService;

    @Override
    public ItemResponse findById(Long id) {
        return itemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Item not found with id: " + id
                        )
                );
    }

    @Override
    public Page<ItemResponse> findAll(DefaultPaginationRequest paginationRequest) {
        Pageable pageable = PaginationUtils.createPageable(paginationRequest, PaginationFields.ITEM);

        return itemRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ItemResponse addItem(Long productId, Integer quantity) {
        Product product = productService.getProduct(productId);

        Item item = Item.builder()
                .product(product)
                .quantity(quantity)
                .build();

        Item savedItem = itemRepository.save(item);

        return toResponse(savedItem);
    }

    @Override
    public List<ItemResponse> findItemsByProductId(Long productId) {
        productService.getProduct(productId);

        return itemRepository
                .findByProductIdOrderByIdAsc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getQuantity()
        );
    }
}
