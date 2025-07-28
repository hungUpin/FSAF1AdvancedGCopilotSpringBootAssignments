package com.example.copilot.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.example.copilot.entity.Product;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.entity.Category;
import com.example.copilot.repository.CategoryRepository;
import org.springframework.data.domain.Pageable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void whenSearchProducts_thenReturnMatchingProducts() {
        // Clean up tables to ensure a known state
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        // Arrange: save a unique test category and product
        String uniqueCategory = "Electronics-" + System.currentTimeMillis();
        Category category = new Category();
        category.setName(uniqueCategory);
        categoryRepository.save(category);

        Product laptop = new Product();
        laptop.setName("Gaming Laptop");
        laptop.setPrice(1499.99);
        laptop.setStock(10);
        laptop.setCategory(category);
        productRepository.save(laptop);

        // Act
        Page<Product> results = productRepository.searchProducts(
            "Laptop", // keyword
            category.getId(), // categoryId
            null, // minPrice
            1500.00, // maxPrice
            PageRequest.of(0, 5)
        );

        // Assert
        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getName()).isEqualTo("Gaming Laptop");
        assertThat(results.getContent().get(0).getCategory().getName()).isEqualTo(uniqueCategory);
    }
}
