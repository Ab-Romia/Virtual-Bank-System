package com.virtualbank.user.controller;

import com.virtualbank.user.dto.UserRegistrationRequest;
import com.virtualbank.user.dto.UserRegistrationResponse;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

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
    @GetMapping("/register")
    public ResponseEntity<Map<String, String>> getRegisterInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "To register a new user, send a POST request to this endpoint with the required user information.");
        response.put("requiredFields", "username, email, password, firstName, lastName");
        return ResponseEntity.ok(response);
    }
}