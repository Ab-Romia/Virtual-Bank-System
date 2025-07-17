package com.virtualbank.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.user.dto.UserRegistrationRequest;
import com.virtualbank.user.dto.UserRegistrationResponse;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void registerUser_Success() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        UUID userId = UUID.randomUUID();
        UserRegistrationResponse response = UserRegistrationResponse.builder()
                .userId(userId)
                .username("testuser")
                .message("User registered successfully.")
                .build();

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.message").value("User registered successfully."));
    }

    @Test
    @WithMockUser
    void registerUser_UserAlreadyExists() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("existinguser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("Existing");
        request.setLastName("User");

        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username or email already exists"));

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username or email already exists"));
    }

    @Test
    @WithMockUser
    void registerUser_ValidationFailure() throws Exception {
        // Arrange - Empty request to trigger validation errors
        UserRegistrationRequest request = new UserRegistrationRequest();

        // Act & Assert
        mockMvc.perform(post("/users/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}