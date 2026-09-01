package com.example.productapi.service;

import com.example.productapi.dto.common.DefaultPaginationRequest;
import com.example.productapi.dto.item.ItemResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ItemService {

    ItemResponse findById(Long id);

    Page<ItemResponse> findAll(DefaultPaginationRequest paginationRequest);

    ItemResponse addItem(Long productId, Integer quantity);

    List<ItemResponse> findItemsByProductId(Long productId);
}
