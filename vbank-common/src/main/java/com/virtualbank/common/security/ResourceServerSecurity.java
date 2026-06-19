package com.virtualbank.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Default zero-trust resource-server configuration for the servlet services:
 * stateless, CSRF off (no cookies), every request authenticated by a validated
 * RS256 JWT except health, metrics, and API docs. The token is validated against
 * user-service's JWKS via {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}.
 *
 * <p>Services that need their own public endpoints (for example user-service's
 * register, login, and JWKS) declare their own {@link SecurityFilterChain}; this
 * default backs off via {@link ConditionalOnMissingBean}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerSecurity {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
