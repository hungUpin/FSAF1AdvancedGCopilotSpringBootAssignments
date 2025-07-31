package com.example.copilot.controller;

import com.example.copilot.entity.Category;
import com.example.copilot.entity.Product;
import com.example.copilot.repository.CategoryRepository;
import com.example.copilot.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@org.springframework.test.context.jdbc.Sql(scripts = "/truncate.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Clear both repositories to ensure clean state
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        // Create test category
        testCategory = new Category();
        testCategory.setName("Electronics");
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCreateProduct() throws Exception {
        // Arrange
        var productRequest = new java.util.HashMap<String, Object>();
        productRequest.put("name", "Test Product");
        productRequest.put("price", 99.99);
        productRequest.put("stock", 10);
        // Send nested category object instead of categoryId
        var categoryMap = new java.util.HashMap<String, Object>();
        categoryMap.put("id", testCategory.getId());
        productRequest.put("category", categoryMap);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.price").value(99.99))
            .andExpect(jsonPath("$.stock").value(10));

        // Verify product was created
        assertThat(productRepository.count()).isEqualTo(1);
        Product savedProduct = productRepository.findAll().get(0);
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        assertThat(savedProduct.getPrice()).isCloseTo(99.99, within(0.01));
        assertThat(savedProduct.getStock()).isEqualTo(10);
        assertThat(savedProduct.getCategory().getId()).isEqualTo(testCategory.getId());
    }

    @Test
    void testCreateProductWithInvalidData() throws Exception {
        // Arrange
        var productRequest = new java.util.HashMap<String, Object>();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateProductWithNegativePrice() throws Exception {
        // Arrange
        var productRequest = new java.util.HashMap<String, Object>();
        productRequest.put("name", "Test Product");
        productRequest.put("price", -10.0);
        productRequest.put("stock", 10);
        var categoryMap = new java.util.HashMap<String, Object>();
        categoryMap.put("id", testCategory.getId());
        productRequest.put("category", categoryMap);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateProductWithNegativeStock() throws Exception {
        // Arrange
        var productRequest = new java.util.HashMap<String, Object>();
        productRequest.put("name", "Test Product");
        productRequest.put("price", 99.99);
        productRequest.put("stock", -1);
        var categoryMap = new java.util.HashMap<String, Object>();
        categoryMap.put("id", testCategory.getId());
        productRequest.put("category", categoryMap);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
            .andExpect(status().isInternalServerError());
    }
}
