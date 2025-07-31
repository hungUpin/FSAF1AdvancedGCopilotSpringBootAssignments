package com.example.copilot.security;

import com.example.copilot.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc 
@ActiveProfiles("test")
@DisplayName("Fixed Security Tests")
public class FixedSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Authentication endpoints should be accessible")
    void testAuthEndpointsAccessible() throws Exception {
        // Test login endpoint is accessible (should return 401 for invalid credentials)
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoints should require authentication")
    void testProtectedEndpoints() throws Exception {
        // User management should be protected
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
                
        // User creation should be protected
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"test@test.com\",\"role\":\"USER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public product endpoints should be accessible")
    void testPublicProductEndpoints() throws Exception {
        // Product listing should be public
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Invalid JWT should be rejected")
    void testInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Email validation should work")
    void testEmailValidation() throws Exception {
        LoginRequest invalidEmail = new LoginRequest("not-an-email", "password");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CORS should be configured")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dashboard should be protected")
    void testDashboardProtection() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
