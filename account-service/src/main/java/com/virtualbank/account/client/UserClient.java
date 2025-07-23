package com.virtualbank.account.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserClient(RestTemplate restTemplate,
                      @Value("${user.service.url:http://localhost:8081}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public boolean validateUserExists(UUID userId) {
        try {
            log.info("Validating user existence: {}", userId);
            restTemplate.getForObject(userServiceUrl + "/users/{userId}/profile", Object.class, userId);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error validating user: {}", e.getMessage(), e);
            throw new RuntimeException("Error validating user", e);
        }
    }
}