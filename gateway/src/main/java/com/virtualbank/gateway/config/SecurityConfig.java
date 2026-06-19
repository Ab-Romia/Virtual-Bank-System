package com.virtualbank.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive zero-trust security for the gateway. The chain is stateless and CSRF is
 * disabled (no browser cookies are used; clients send a Bearer token). Registration
 * and login are public so unauthenticated callers can obtain a token; the actuator
 * health probes are public for orchestration. Every other route requires a valid
 * RS256 JWT, validated against user-service's JWKS configured via
 * {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // CORS is handled by a dedicated CorsWebFilter bean; let it own the policy.
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        // Public: register and login must be reachable without a token.
                        .pathMatchers("/api/auth/**").permitAll()
                        // Public: health probes for container orchestration.
                        .pathMatchers("/actuator/health/**").permitAll()
                        // CORS preflight requests carry no credentials.
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

}
