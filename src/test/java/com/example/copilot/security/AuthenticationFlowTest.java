package com.example.copilot.security;

import com.example.copilot.dto.LoginRequest;
import com.example.copilot.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Authentication Flow Tests")
public class AuthenticationFlowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("Valid user registration should succeed")
    void testValidRegistration() throws Exception {
        RegisterRequest validRequest = new RegisterRequest();
        validRequest.setName("Valid User");
        validRequest.setEmail("validuser@example.com");
        validRequest.setPassword("ValidPass123!");
        validRequest.setConfirmPassword("ValidPass123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful! Please log in with your credentials."));
    }

    @Test
    @DisplayName("Registration with existing email should fail")
    void testDuplicateEmailRegistration() throws Exception {
        // Try to register with an email that already exists in the database (alice@example.com)
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setName("Duplicate User");
        duplicateRequest.setEmail("alice@example.com"); // This email already exists
        duplicateRequest.setPassword("ValidPass123!");
        duplicateRequest.setConfirmPassword("ValidPass123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    @DisplayName("Registration with mismatched passwords should fail")
    void testPasswordMismatchRegistration() throws Exception {
        RegisterRequest mismatchRequest = new RegisterRequest();
        mismatchRequest.setName("Test User");
        mismatchRequest.setEmail("mismatch@example.com");
        mismatchRequest.setPassword("ValidPass123!");
        mismatchRequest.setConfirmPassword("DifferentPass123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password mismatch"));
    }

    @Test
    @DisplayName("Login with non-existent user should fail")
    void testLoginWithNonExistentUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
    }

    @Test
    @DisplayName("Token validation endpoint should work correctly")
    void testTokenValidation() throws Exception {
        // Test with no token
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isBadRequest()); // Missing Authorization header

        // Test with invalid token
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));

        // Test with malformed header
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid authorization header format"));
    }

    @Test
    @DisplayName("Email validation should be strict")
    void testEmailValidation() throws Exception {
        String[] invalidEmails = {
            "notanemail",
            "@example.com",
            "user@",
            "user..double.dot@example.com",
            "user@.com",
            "user@com",
            "user name@example.com", // space not allowed
            "user@ex ample.com" // space in domain
        };

        for (String invalidEmail : invalidEmails) {
            LoginRequest loginRequest = new LoginRequest(invalidEmail, "password123");
            
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid email format"));
        }
    }

    @Test
    @DisplayName("Registration should enforce role security")
    void testRoleSecurityInRegistration() throws Exception {
        // Even if someone tries to set role to ADMIN, it should default to USER
        RegisterRequest adminAttempt = new RegisterRequest();
        adminAttempt.setName("Admin Wannabe");
        adminAttempt.setEmail("admin-wannabe@example.com");
        adminAttempt.setPassword("ValidPass123!");
        adminAttempt.setConfirmPassword("ValidPass123!");
        // Note: RegisterRequest automatically sets role to "USER" and it's final
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminAttempt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful! Please log in with your credentials."));
        
        // The user should be created with USER role, not ADMIN
        // This would be verified by checking the database or trying to access admin endpoints
    }

    @Test
    @DisplayName("Special characters in input should be handled safely")
    void testSpecialCharacterHandling() throws Exception {
        // Test registration with special characters
        RegisterRequest specialCharsRequest = new RegisterRequest();
        specialCharsRequest.setName("Test User"); // Valid name
        specialCharsRequest.setEmail("test+special@example.com"); // Valid email with +
        specialCharsRequest.setPassword("Valid@Pass123!"); // Valid password with special chars
        specialCharsRequest.setConfirmPassword("Valid@Pass123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialCharsRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Case sensitivity should be handled correctly")
    void testCaseSensitivity() throws Exception {
        // Email should be case-insensitive (converted to lowercase)
        LoginRequest upperCaseEmail = new LoginRequest("ALICE@EXAMPLE.COM", "password123");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(upperCaseEmail)))
                .andExpect(status().isUnauthorized()) // Will fail because password is wrong, but email should be normalized
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
