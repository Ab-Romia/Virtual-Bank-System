package com.virtualbank.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.user.dto.UserProfileResponse;
import com.virtualbank.user.dto.UserRegistrationRequest;
import com.virtualbank.user.dto.UserRegistrationResponse;
import com.virtualbank.user.dto.UserLoginRequest;
import com.virtualbank.user.dto.UserLoginResponse;
import com.virtualbank.user.exception.InvalidCredentialsException;
import com.virtualbank.user.exception.UserAlreadyExistsException;
import com.virtualbank.user.exception.UserNotFoundException;
import com.virtualbank.user.service.UserService;
import com.virtualbank.user.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper; // For converting objects to JSON strings


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request,
                                          @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {
        sendLogToKafka("Request", "/users/register", request, appName);

        try {
            UserRegistrationResponse response = userService.registerUser(request);
            sendLogToKafka("Response", "/users/register", response, appName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserAlreadyExistsException e) {
            Map<String, Object> errorResponse = createErrorResponse(409, "Conflict", e.getMessage());
            sendLogToKafka("Response", "/users/register", errorResponse, appName);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request,
                                   @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {
        sendLogToKafka("Request", "/users/login", request, appName);

        try {
            UserLoginResponse response = userService.login(request);
            sendLogToKafka("Response", "/users/login", response, appName);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (InvalidCredentialsException e) {
            Map<String, Object> errorResponse = createErrorResponse(401, "Unauthorized", e.getMessage());
            sendLogToKafka("Response", "/users/login", errorResponse, appName);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable UUID userId,
                                            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {
        String endpoint = "/users/" + userId + "/profile";
        sendLogToKafka("Request", endpoint, "Get profile for userId: " + userId, appName);

        try {
            UserProfileResponse response = userService.getUserProfile(userId);
            sendLogToKafka("Response", endpoint, response, appName);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            Map<String, Object> errorResponse = createErrorResponse(404, "Not Found", e.getMessage());
            sendLogToKafka("Response", endpoint, errorResponse, appName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    private Map<String, Object> createErrorResponse(int status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }


    @SneakyThrows
    private void sendLogToKafka(String messageType, String endpoint, Object payload, String appName) {
        Map<String, Object> log = new HashMap<>();
        log.put("message", objectMapper.writeValueAsString(payload));
        log.put("messageType", messageType);
        log.put("dateTime", OffsetDateTime.now());
        log.put("sourceService", "user-service");
        log.put("sourceEndpoint", endpoint);
        log.put("appName", appName);
        kafkaProducerService.sendLog(log);
    }
}