package com.virtualbank.gateway.dashboard;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Backend-for-frontend aggregation for the dashboard view. It composes a single
 * response for the authenticated caller from the backing services. Today it fetches
 * the user profile from user-service; accounts and recent transactions are reserved
 * for when those services exist. A downstream failure degrades gracefully: the
 * dashboard returns whatever is available rather than failing the whole request.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final WebClient userServiceWebClient;

    public DashboardController(WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DashboardResponse> dashboard(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        return fetchProfile(authorization).map(DashboardResponse::of);
    }

    /**
     * Calls user-service for the caller's profile, propagating the caller's bearer
     * token. Any error (service down, non-2xx, timeout) resolves to an empty profile
     * so the dashboard still renders the parts that succeeded.
     */
    private Mono<Object> fetchProfile(String authorization) {
        WebClient.RequestHeadersSpec<?> request = userServiceWebClient.get()
                .uri("/users/me");
        if (authorization != null && !authorization.isBlank()) {
            request = request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return request.retrieve()
                .bodyToMono(Object.class)
                .onErrorResume(error -> Mono.empty());
    }

}
