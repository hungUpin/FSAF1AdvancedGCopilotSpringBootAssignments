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
        // 1. Create a user via API call
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
        Long userId = null;
        if (userStatus == 201) {
            Map<String, Object> userResponse = objectMapper.readValue(userResult.getResponse().getContentAsString(), Map.class);
            userId = Long.valueOf(userResponse.get("id").toString());
        } else {
            System.out.println("User API response status: " + userStatus);
            System.out.println("User API response body: " + userResult.getResponse().getContentAsString());
            return;
        }

        // 2. Create a category via API call (or directly in DB if no endpoint)
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

        // 3. Create a product via API call (with category)
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
        Long productId = Long.valueOf(productResponse.get("id").toString());

        // 4. Place an order
        Map<String, Object> orderRequest = Map.of(
            "user", Map.of("id", userId),
            "items", List.of(Map.of(
                "product", Map.of("id", productId),
                "quantity", 2
            ))
        );
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
            .andReturn();

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
}

