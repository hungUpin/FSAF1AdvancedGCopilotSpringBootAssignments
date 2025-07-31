package com.example.copilot.controller;

import com.example.copilot.entity.Category;
import com.example.copilot.entity.Order;
import com.example.copilot.entity.OrderStatus;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.User;
import com.example.copilot.repository.CategoryRepository;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Create test category
        testCategory = new Category();
        testCategory.setName("Electronics");
        testCategory = categoryRepository.save(testCategory);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setPrice(99.99);
        testProduct.setStock(10);
        testProduct.setCategory(testCategory);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testPlaceOrderEndpoint() throws Exception {
        // Arrange
        Map<String, Object> orderRequest = Map.of(
            "userId", testUser.getId(),
            "items", List.of(Map.of(
                "productId", testProduct.getId(),
                "quantity", 2
            ))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isCreated());

        // Verify order was created
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orders.get(0).getUser().getId()).isEqualTo(testUser.getId());
        assertThat(orders.get(0).getOrderItems()).hasSize(1);
        assertThat(orders.get(0).getOrderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(orders.get(0).getOrderItems().get(0).getProduct().getId()).isEqualTo(testProduct.getId());

        // Verify product stock was updated
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8); // Original 10 - 2
    }

    @Test
    void testPlaceOrderWithInvalidUser() throws Exception {
        // Arrange
        Map<String, Object> orderRequest = Map.of(
            "userId", 999L,
            "items", List.of(Map.of(
                "productId", testProduct.getId(),
                "quantity", 2
            ))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isNotFound());

        // Verify no order was created
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void testPlaceOrderWithInvalidProduct() throws Exception {
        // Arrange
        Map<String, Object> orderRequest = Map.of(
            "userId", testUser.getId(),
            "items", List.of(Map.of(
                "productId", 999L,
                "quantity", 2
            ))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isNotFound());

        // Verify no order was created
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void testPlaceOrderWithInsufficientStock() throws Exception {
        // Arrange
        Map<String, Object> orderRequest = Map.of(
            "userId", testUser.getId(),
            "items", List.of(Map.of(
                "productId", testProduct.getId(),
                "quantity", 20 // More than available stock (10)
            ))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isInternalServerError());

        // Verify no order was created and stock wasn't changed
        assertThat(orderRepository.count()).isZero();
        Product unchangedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(10); // Stock should remain unchanged
    }

    @Test
    void testCreateOrder() throws Exception {
        var orderRequest = new java.util.HashMap<String, Object>();
        orderRequest.put("productId", testProduct.getId());
        orderRequest.put("quantity", 1);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(0);
    }
}
