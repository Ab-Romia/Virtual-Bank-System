package com.virtualbank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Public entry point for the Virtual Bank System. This reactive gateway validates
 * RS256 JWTs against user-service's JWKS, routes traffic to the backend services,
 * applies CORS, and exposes an aggregated dashboard endpoint.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
