package com.example.copilot.workflow;

import com.example.copilot.entity.Order;
import com.example.copilot.repository.OrderRepository;
import com.example.copilot.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback changes after each test
public class ECommerceWorkflowTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void testPlaceOrderWorkflow() throws Exception {
        // Arrange: Setup test data (User, Category, and Product)
        TestDataSetup testData = setupInitialData();
        
        // Act: Place an order using the created test data
        Map<String, Object> orderRequest = Map.of(
            "user", Map.of("id", testData.getUserId()),
            "items", List.of(Map.of(
                "product", Map.of("id", testData.getProductId()),
                "quantity", 2
            ))
        );
        
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andReturn();

        // Assert: Verify the order creation was successful
        int status = orderResult.getResponse().getStatus();
        if (status != 201 && status != 200) {
            System.out.println("Order API response status: " + status);
            System.out.println("Order API response body: " + orderResult.getResponse().getContentAsString());
        }
        assertThat(status)
            .withFailMessage("Expected HTTP 201 or 200 but got %s. Body: %s", status, orderResult.getResponse().getContentAsString())
            .isIn(201, 200);

        // Assert: Verify the order was created in the database
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo("PENDING");
    }

    /**
     * Sets up initial test data by creating a User, Category, and Product via API calls.
     * This method encapsulates the complex "Arrange" phase of the test.
     * 
     * @return TestDataSetup object containing the IDs of created entities
     * @throws Exception if any API call fails
     */
    private TestDataSetup setupInitialData() throws Exception {
        // 1. Create a user via API call
        Long userId = createTestUser();
        
        // 2. Create a category via API call
        Long categoryId = createTestCategory();
        
        // 3. Create a product via API call (linked to the category)
        Long productId = createTestProduct(categoryId);
        
        return new TestDataSetup(userId, categoryId, productId);
    }
    
    /**
     * Creates a test user via API call.
     * 
     * @return the ID of the created user
     * @throws Exception if the API call fails
     */
    private Long createTestUser() throws Exception {
        Map<String, Object> user = Map.of(
            "name", "Test User",
            "email", "testuser@example.com"
        );
        
        MvcResult userResult = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andReturn();
            
        int userStatus = userResult.getResponse().getStatus();
        assertThat(userStatus)
            .withFailMessage("Expected HTTP 201 or 400 but got %s. Body: %s", userStatus, userResult.getResponse().getContentAsString())
            .isIn(201, 400);
            
        if (userStatus == 201) {
            Map<String, Object> userResponse = objectMapper.readValue(userResult.getResponse().getContentAsString(), Map.class);
            return Long.valueOf(userResponse.get("id").toString());
        } else {
            System.out.println("User API response status: " + userStatus);
            System.out.println("User API response body: " + userResult.getResponse().getContentAsString());
            throw new RuntimeException("Failed to create test user");
        }
    }
    
    /**
     * Creates a test category via API call.
     * 
     * @return the ID of the created category
     * @throws Exception if the API call fails
     */
    private Long createTestCategory() throws Exception {
        Map<String, Object> category = Map.of(
            "name", "Electronics"
        );
        
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
            .andReturn();
            
        int categoryStatus = categoryResult.getResponse().getStatus();
        if (categoryStatus != 201) {
            System.out.println("Category API response status: " + categoryStatus);
            System.out.println("Category API response body: " + categoryResult.getResponse().getContentAsString());
        }
        assertThat(categoryStatus)
            .withFailMessage("Expected HTTP 201 but got %s. Body: %s", categoryStatus, categoryResult.getResponse().getContentAsString())
            .isEqualTo(201);
            
        Map<String, Object> categoryResponse = objectMapper.readValue(categoryResult.getResponse().getContentAsString(), Map.class);
        Long categoryId = Long.valueOf(categoryResponse.get("id").toString());
        
        // Ensure category is persisted before creating product
        categoryRepository.flush();
        
        return categoryId;
    }
    
    /**
     * Creates a test product via API call, linked to the specified category.
     * 
     * @param categoryId the ID of the category to link the product to
     * @return the ID of the created product
     * @throws Exception if the API call fails
     */
    private Long createTestProduct(Long categoryId) throws Exception {
        Map<String, Object> product = Map.of(
            "name", "Test Product",
            "price", 99.99,
            "stock", 10,
            "category", Map.of("id", categoryId)
        );
        
        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
            .andExpect(status().isCreated())
            .andReturn();
            
        Map<String, Object> productResponse = objectMapper.readValue(productResult.getResponse().getContentAsString(), Map.class);
        return Long.valueOf(productResponse.get("id").toString());
    }
    
    /**
     * Data wrapper class to hold the IDs of created test entities.
     * This provides a clean way to return multiple values from the setup method.
     */
    private static class TestDataSetup {
        private final Long userId;
        private final Long categoryId;
        private final Long productId;
        
        public TestDataSetup(Long userId, Long categoryId, Long productId) {
            this.userId = userId;
            this.categoryId = categoryId;
            this.productId = productId;
        }
        
        public Long getUserId() { return userId; }
        public Long getCategoryId() { return categoryId; }
        public Long getProductId() { return productId; }
        
        @Override
        public String toString() {
            return String.format("TestDataSetup{userId=%d, categoryId=%d, productId=%d}", 
                userId, categoryId, productId);
        }
    }
}

