package com.example.copilot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.copilot.entity.base.Auditable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    // Standard B-tree indexes for exact matches and range queries
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_category_price", columnList = "category_id, price"),
    @Index(name = "idx_product_price", columnList = "price"),
    @Index(name = "idx_product_stock", columnList = "stock"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    
    // Composite index for search optimization with filters
    @Index(name = "idx_product_search_composite", columnList = "category_id, price, name")
})
// Full-text search index for MySQL/PostgreSQL - significantly improves LIKE performance
@org.hibernate.annotations.NamedNativeQueries({
    @org.hibernate.annotations.NamedNativeQuery(
        name = "Product.createFullTextIndex",
        query = "CREATE FULLTEXT INDEX idx_product_fulltext ON products (name, description)",
        resultClass = Product.class
    )
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Product name is required")
    // Add collation for case-insensitive searches (MySQL specific)
    @org.hibernate.annotations.Collate("utf8mb4_unicode_ci")
    private String name;

    @Column(columnDefinition = "TEXT")
    // Add collation for case-insensitive searches (MySQL specific)
    @org.hibernate.annotations.Collate("utf8mb4_unicode_ci")
    private String description;

    @Column(nullable = false)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    @Column(nullable = false)
    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;
}
