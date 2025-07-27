package com.virtualbank.bff_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.bff_service.service.DashboardService;
import com.virtualbank.bff_service.service.KafkaProducerService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/bff")
public class DashboardController {
    private final DashboardService dashboardService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DashboardController(DashboardService dashboardService,
                               KafkaProducerService kafkaProducerService,
                               ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard/{userId}")
    public Mono<ResponseEntity<Object>> getUserDashboard(@PathVariable String userId,
                                                         @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {
        String endpoint = "/bff/dashboard/" + userId;
        sendLogToKafka("Request", endpoint, "Get dashboard for userId: " + userId, appName);

        try {
            UUID userUuid = UUID.fromString(userId);
            return dashboardService.getUserDashboard(userUuid)
                    .map(dashboard -> {
                        sendLogToKafka("Response", endpoint, dashboard, appName);
                        return ResponseEntity.ok().body((Object)dashboard);
                    })
                    .onErrorResume(e -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("status", 500);
                        errorResponse.put("error", "Internal Server Error");
                        errorResponse.put("message", "Failed to retrieve dashboard data due to an issue with downstream services.");
                        sendLogToKafka("Response", endpoint, errorResponse, appName);
                        return Mono.just(ResponseEntity.status(500).body((Object)errorResponse));
                    });
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid user ID format");
            sendLogToKafka("Response", endpoint, errorResponse, appName);
            return Mono.just(ResponseEntity.status(400).body((Object)errorResponse));
        }
    }


    @SneakyThrows
    private void sendLogToKafka(String messageType, String endpoint, Object payload, String appName) {
        Map<String, Object> log = new HashMap<>();
        log.put("message", objectMapper.writeValueAsString(payload));
        log.put("messageType", messageType);
        log.put("dateTime", OffsetDateTime.now());
        log.put("sourceService", "bff-service");
        log.put("sourceEndpoint", endpoint);
        log.put("appName", appName);
        kafkaProducerService.sendLog(log);
    }
}