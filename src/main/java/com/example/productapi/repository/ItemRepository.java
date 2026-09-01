package com.example.productapi.repository;

import com.example.productapi.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByProductIdOrderByIdAsc(Long productId);
}
