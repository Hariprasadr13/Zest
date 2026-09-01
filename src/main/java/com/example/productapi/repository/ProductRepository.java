package com.example.productapi.repository;

import com.example.productapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAll(Pageable pageable);
}
