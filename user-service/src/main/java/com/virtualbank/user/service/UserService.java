package com.virtualbank.user.service;

import com.virtualbank.user.dto.*;
import com.virtualbank.user.entity.User;
import com.virtualbank.user.exception.InvalidCredentialsException;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.exception.UserNotFoundException;
import com.virtualbank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        try {
            if (userRepository.existsByUsernameOrEmail(request.getUsername(), request.getEmail())) {
                throw new UserAlreadyExistsException("Username or email already exists");
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .isActive(true)
                    .build();

            User savedUser = userRepository.save(user);
            logger.info("User saved successfully: {}", savedUser.getUserId());
            return UserRegistrationResponse.builder()
                    .userId(savedUser.getUserId())
                    .username(savedUser.getUsername())
                    .message("User registered successfully.")
                    .build();
        } catch (Exception e) {
            logger.error("Error saving user: {}", e.getMessage(), e);
            throw e;
        }
    }
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .isActive(user.getIsActive())
                .build();
    }


    @Transactional
    public UserLoginResponse login(UserLoginRequest request){
        try {
            return userRepository.findByUsername(request.getUsername())
                    .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                    .map(user -> new UserLoginResponse(user.getUserId(), user.getUsername()))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid Credentials"));
        } catch (Exception e) {
            logger.error("Error logging in: {}", e.getMessage(), e);
            throw e;
        }
    }

}