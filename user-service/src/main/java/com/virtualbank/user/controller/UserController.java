package com.virtualbank.user.controller;

import com.virtualbank.user.dto.UserProfileResponse;
import com.virtualbank.user.dto.UserRegistrationRequest;
import com.virtualbank.user.dto.UserRegistrationResponse;
import com.virtualbank.user.dto.UserLoginRequest;
import com.virtualbank.user.dto.UserLoginResponse;
import com.virtualbank.user.exception.InvalidCredentialsException;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.exception.UserNotFoundException;
import com.virtualbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            UserRegistrationResponse response = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserAlreadyExistsException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 409);
            errorResponse.put("error", "Conflict");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable UUID userId) {
        try {
            UserProfileResponse response = userService.getUserProfile(userId);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 404);
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request) {
        try {
            UserLoginResponse response = userService.login(request);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch(InvalidCredentialsException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 401);
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}