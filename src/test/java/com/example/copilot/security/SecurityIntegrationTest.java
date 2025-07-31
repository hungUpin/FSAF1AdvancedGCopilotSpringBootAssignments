package com.example.copilot.security;

import com.example.copilot.dto.LoginRequest;
import com.example.copilot.dto.RegisterRequest;
import com.example.copilot.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Security Integration Tests")
public class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Nested
    @DisplayName("Authentication Endpoint Security")
    class AuthenticationTests {

        @Test
        @DisplayName("Should allow access to login endpoint without authentication")
        void testLoginEndpointAccessible() throws Exception {
            LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
            
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should allow access to registration endpoint without authentication")
        void testRegistrationEndpointAccessible() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setName("Test User");
            registerRequest.setEmail("newuser@example.com");
            registerRequest.setPassword("password123"); // Invalid password
            registerRequest.setConfirmPassword("password123");
            
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest()) // Should fail validation but be accessible
                    .andExpect(jsonPath("$.error").value("Validation failed"));
        }

        @Test
        @DisplayName("Should validate input on registration")
        void testRegistrationInputValidation() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest();
            invalidRequest.setName(""); // Invalid: empty name
            invalidRequest.setEmail("invalid-email"); // Invalid: bad email format
            invalidRequest.setPassword("weak"); // Invalid: weak password
            invalidRequest.setConfirmPassword("different"); // Invalid: passwords don't match
            
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation failed"));
        }
    }

    @Nested
    @DisplayName("Admin Endpoint Protection")
    class AdminEndpointTests {

        @Test
        @DisplayName("Should deny unauthorized access to user management")
        void testUserManagementRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Access denied. Please provide valid authentication credentials."));
        }

        @Test
        @DisplayName("Should deny unauthorized access to dashboard statistics")
        void testDashboardRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/stats"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }

        @Test
        @DisplayName("Should deny unauthorized POST to user creation")
        void testUserCreationRequiresAuth() throws Exception {
            UserDTO userDTO = new UserDTO();
            userDTO.setName("New User");
            userDTO.setEmail("new@example.com");
            userDTO.setRole("USER");
            
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deny unauthorized DELETE to user deletion")
        void testUserDeletionRequiresAuth() throws Exception {
            mockMvc.perform(delete("/api/v1/users/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Public Endpoint Access")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should allow public access to product listing")
        void testProductListingIsPublic() throws Exception {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Should allow public access to category listing")
        void testCategoryListingIsPublic() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow public access to individual product")
        void testIndividualProductIsPublic() throws Exception {
            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").exists());
        }

        @Test
        @DisplayName("Should allow public access to product reviews")
        void testProductReviewsArePublic() throws Exception {
            mockMvc.perform(get("/api/v1/reviews/product/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Endpoint Access")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Should deny unauthorized access to order creation")
        void testOrderCreationRequiresAuth() throws Exception {
            String orderJson = """
                {
                    "userId": 1,
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;
            
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deny unauthorized access to user orders")
        void testUserOrdersRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/orders/user/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deny unauthorized access to review creation")
        void testReviewCreationRequiresAuth() throws Exception {
            String reviewJson = """
                {
                    "productId": 1,
                    "userId": 1,
                    "rating": 5,
                    "content": "Great product!"
                }
                """;
            
            mockMvc.perform(post("/api/v1/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewJson))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CORS and Security Headers")
    class SecurityHeaderTests {

        @Test
        @DisplayName("Should handle OPTIONS requests for CORS")
        void testCorsPreflightRequest() throws Exception {
            mockMvc.perform(options("/api/v1/products")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject invalid JWT tokens")
        void testInvalidJwtTokenRejection() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                    .header("Authorization", "Bearer invalid-jwt-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject malformed Authorization headers")
        void testMalformedAuthHeaderRejection() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                    .header("Authorization", "InvalidFormat token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Input Validation Security")
    class InputValidationTests {

        @Test
        @DisplayName("Should validate email format in login")
        void testLoginEmailValidation() throws Exception {
            LoginRequest invalidLogin = new LoginRequest("not-an-email", "password123");
            
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidLogin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation failed"));
        }

        @Test
        @DisplayName("Should enforce password strength requirements")
        void testPasswordStrengthValidation() throws Exception {
            RegisterRequest weakPasswordRequest = new RegisterRequest();
            weakPasswordRequest.setName("Test User");
            weakPasswordRequest.setEmail("test@example.com");
            weakPasswordRequest.setPassword("weak"); // Too short, no uppercase, no special chars
            weakPasswordRequest.setConfirmPassword("weak");
            
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation failed"));
        }

        @Test
        @DisplayName("Should prevent SQL injection attempts")
        void testSqlInjectionPrevention() throws Exception {
            LoginRequest sqlInjectionAttempt = new LoginRequest("admin' OR '1'='1", "password");
            
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sqlInjectionAttempt)))
                    .andExpect(status().isUnauthorized()) // Should fail authentication, not cause SQL error
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }
    }
}
