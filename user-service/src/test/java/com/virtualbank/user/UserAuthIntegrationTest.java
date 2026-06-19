package com.virtualbank.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.virtualbank.user.web.dto.LoginRequest;
import com.virtualbank.user.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 proof for user-service: register and login issue a working RS256 token,
 * the token authenticates /users/me, a missing token is rejected, and one user
 * cannot read another user's profile by id (the IDOR the audit flagged).
 *
 * Postgres comes from Testcontainers so Flyway runs against the real schema and
 * ddl-auto=validate confirms the entity matches. Kafka autoconfiguration is excluded
 * so no broker is needed; the shared AuditPublisher is filtered out of the scan
 * because audit is not on the auth path in Phase 1.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@Testcontainers
class UserAuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerLoginAndOwnershipFlow() {
        // 1. Register user A and log in: the token must be present.
        register("alice", "alice@example.com", "alice-secret", "Alice Anderson");
        String aliceToken = login("alice", "alice-secret");
        assertThat(aliceToken).isNotBlank();

        // 2. The token authenticates /users/me and returns A's username.
        ResponseEntity<JsonNode> me = rest.exchange("/users/me", HttpMethod.GET,
                new HttpEntity<>(bearer(aliceToken)), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().get("username").asText()).isEqualTo("alice");
        String aliceId = me.getBody().get("id").asText();

        // 3. No token: unauthorized.
        ResponseEntity<String> anonymous = rest.exchange("/users/me", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. User B may not read A's profile by id.
        register("bob", "bob@example.com", "bob-secret", "Bob Brown");
        String bobToken = login("bob", "bob-secret");
        ResponseEntity<String> crossRead = rest.exchange("/users/{id}", HttpMethod.GET,
                new HttpEntity<>(bearer(bobToken)), String.class, aliceId);
        assertThat(crossRead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void register(String username, String email, String password, String fullName) {
        ResponseEntity<JsonNode> response = rest.postForEntity("/auth/register",
                new RegisterRequest(username, email, password, fullName), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String login(String username, String password) {
        ResponseEntity<JsonNode> response = rest.postForEntity("/auth/login",
                new LoginRequest(username, password), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().get("accessToken").asText();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
