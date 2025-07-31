package com.example.copilot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.copilot.entity.base.Auditable;

@Entity
@Table(name = "reviews", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Review extends Auditable<User> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "Review content is required")
    @Size(min = 10, max = 1000, message = "Review content must be between 10 and 1000 characters")
    private String content;

    @Column(nullable = false)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
}
