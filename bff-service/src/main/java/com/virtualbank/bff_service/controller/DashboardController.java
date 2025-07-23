package com.virtualbank.bff_service.controller;

import com.virtualbank.bff_service.dto.DashboardResponse;
import com.virtualbank.bff_service.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bff")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/{userId}")
    public Mono<ResponseEntity<Object>> getUserDashboard(@PathVariable String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return dashboardService.getUserDashboard(userUuid)
                    .map(dashboard -> ResponseEntity.ok().body((Object)dashboard))
                    .onErrorResume(e -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("status", 500);
                        errorResponse.put("error", "Internal Server Error");
                        errorResponse.put("message", "Failed to retrieve dashboard data due to an issue with downstream services.");
                        return Mono.just(ResponseEntity.status(500).body((Object)errorResponse));
                    });
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid user ID format");
            return Mono.just(ResponseEntity.status(400).body((Object)errorResponse));
        }
    }
}