package com.example.copilot.controller;

import com.example.copilot.dto.UserDTO;
import com.example.copilot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
public class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void whenCreateUserWithInvalidEmail_thenReturns400() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Test User");
        userDTO.setEmail("invalid-email"); // Invalid email format
        userDTO.setRole("USER");

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenCreateUserWithInvalidRole_thenReturns400() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setRole("INVALID_ROLE"); // Invalid role

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenCreateValidUser_thenReturns201() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setRole("USER");

        UserDTO createdUser = new UserDTO();
        createdUser.setId(1L);
        createdUser.setName(userDTO.getName());
        createdUser.setEmail(userDTO.getEmail());
        createdUser.setRole(userDTO.getRole());

        when(userService.createUser(any(UserDTO.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
