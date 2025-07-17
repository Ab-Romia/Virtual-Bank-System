package com.virtualbank.user.controller;
import com.virtualbank.user.dto.*;
import com.virtualbank.user.exception.InvalidCredentialsException;
import com.virtualbank.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.service.UserService;

import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getUserProfile_Success() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .createdAt(createdAt)
                .isActive(true)
                .build();

        when(userService.getUserProfile(userId)).thenReturn(profileResponse);

        // Act & Assert
        mockMvc.perform(get("/users/{userId}/profile", userId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser
    void getUserProfile_UserNotFound() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        String errorMessage = "User not found with ID: " + userId;

        when(userService.getUserProfile(userId))
                .thenThrow(new UserNotFoundException(errorMessage));

        // Act & Assert
        mockMvc.perform(get("/users/{userId}/profile", userId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void getUserProfile_Unauthorized() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert - No @WithMockUser annotation means no authentication
        mockMvc.perform(get("/users/{userId}/profile", userId))

                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void loginSuccess() throws Exception {
        // Arrange
        UserLoginRequest request = new UserLoginRequest("testuser", "password123");
        UUID userId = UUID.randomUUID();
        UserLoginResponse response = new UserLoginResponse(userId, "testuser");

        when(userService.login(any(UserLoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/users/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()) // Add this line
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }
    @Test
    @WithMockUser
    void loginFailure() throws Exception {
        // Arrange
        UserLoginRequest request = new UserLoginRequest("testuser", "wrongpassword");

        when(userService.login(any(UserLoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid Credentials"));

        // Act & Assert
        mockMvc.perform(post("/users/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()) // Add this line
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid Credentials"));
    }
}