package com.example.productapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item", indexes = @Index(name = "idx_item_product_id", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_product"))
    private Product product;

    @Column(nullable = false)
    private Integer quantity;
}
