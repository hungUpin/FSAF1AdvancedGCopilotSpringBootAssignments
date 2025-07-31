package com.example.copilot.service;

import com.example.copilot.dto.CreateOrderRequestDTO;
import com.example.copilot.entity.Category;
import com.example.copilot.entity.Product;
import com.example.copilot.entity.User;
import com.example.copilot.repository.CategoryRepository;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.ProductRepository;
import com.example.copilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderServiceTransactionIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clear all data
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Create test category
        Category testCategory = new Category();
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
    void testTransactionRollbackOnFailure() {
        // Create an order request
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(testUser.getId());
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(testProduct.getId());
        item.setQuantity(5);
        request.setItems(Arrays.asList(item));

        // Get initial stock
        int initialStock = testProduct.getStock();
        assertEquals(10, initialStock);

        // Try to place order - this should succeed normally
        try {
            orderService.placeOrder(request);
            
            // Check that stock was actually reduced
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertEquals(5, updatedProduct.getStock()); // 10 - 5 = 5
            
            // Check that order was created
            assertEquals(1, orderRepository.count());
            
        } catch (Exception e) {
            fail("Normal order placement should not fail: " + e.getMessage());
        }
    }

    @Test
    void testExceptionPropagation() {
        // Test with invalid user ID to see if exception propagates correctly
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(999L); // Non-existent user
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(testProduct.getId());
        item.setQuantity(5);
        request.setItems(Arrays.asList(item));

        // Get initial stock
        int initialStock = testProduct.getStock();
        assertEquals(10, initialStock);

        // Try to place order with invalid user
        assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });

        // Check that stock was NOT reduced due to transaction rollback
        Product unchangedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, unchangedProduct.getStock()); // Should remain unchanged
        
        // Check that no order was created
        assertEquals(0, orderRepository.count());
    }

    @Test
    void testInsufficientStockDoesNotCreateOrder() {
        // Test insufficient stock scenario
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setUserId(testUser.getId());
        CreateOrderRequestDTO.Item item = new CreateOrderRequestDTO.Item();
        item.setProductId(testProduct.getId());
        item.setQuantity(15); // More than available (10)
        request.setItems(Arrays.asList(item));

        // Get initial stock
        int initialStock = testProduct.getStock();
        assertEquals(10, initialStock);

        // Try to place order with insufficient stock
        assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });

        // Check that stock was NOT reduced
        Product unchangedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, unchangedProduct.getStock()); // Should remain unchanged
        
        // Check that no order was created
        assertEquals(0, orderRepository.count());
    }
}
