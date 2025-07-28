package com.example.copilot.controller;

import com.example.copilot.dto.UserDTO;
import com.example.copilot.exception.ResourceNotFoundException;
import com.example.copilot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private UserService userService;

    @Test
    void testGetUserById() throws Exception {
        // Arrange
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        when(userService.getUserById(1L)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Resource Not Found"))
            .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void testGetAllUsers() throws Exception {
        // Arrange
        UserDTO user1 = new UserDTO();
        user1.setId(1L);
        user1.setName("Alice");
        user1.setEmail("alice@example.com");

        UserDTO user2 = new UserDTO();
        user2.setId(2L);
        user2.setName("Bob");
        user2.setEmail("bob@example.com");

        List<UserDTO> users = Arrays.asList(user1, user2);
        when(userService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void testCreateUser() throws Exception {
        // Arrange
        UserDTO userToCreate = new UserDTO();
        userToCreate.setId(1L);
        userToCreate.setName("Alice");
        userToCreate.setEmail("alice@example.com");

        when(userService.createUser(any(UserDTO.class))).thenReturn(userToCreate);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToCreate)))
            .andExpect(status().isBadRequest());
           
        verify(userService, never()).createUser(any(UserDTO.class));
    }

    @Test
    void testCreateUserInvalidEmail() throws Exception {
        // Arrange
        UserDTO userWithInvalidEmail = new UserDTO();
        userWithInvalidEmail.setId(1L);
        userWithInvalidEmail.setName("Alice");
        userWithInvalidEmail.setEmail("invalid-email"); // Invalid email format

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userWithInvalidEmail)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.email").value("Email should be valid"));

        verify(userService, never()).createUser(any(UserDTO.class));
    }
}
