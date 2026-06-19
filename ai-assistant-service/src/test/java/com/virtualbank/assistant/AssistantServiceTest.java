package com.virtualbank.assistant;

import com.virtualbank.assistant.rag.PolicyRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies everything that can be verified without a live model key.
 *
 * <p>Postgres comes from Testcontainers using the pgvector image so the real
 * pgvector store initializes its schema. The OpenRouter key is left blank, which
 * proves two things at once: retrieval over the embedded policy corpus works with
 * only the local embedding model (no key), and the chat endpoint degrades to a
 * clear "not configured" message instead of failing. The JWKS uri is never
 * reached because the test authenticates with a mock JWT.
 */
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:0/jwks"
})
@AutoConfigureMockMvc
@Testcontainers
class AssistantServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PolicyRetriever policyRetriever;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retrievesRelevantPolicyChunkWithoutAnyApiKey() {
        String retrieved = policyRetriever.retrieve("How much can I transfer in a single transfer?");

        assertThat(retrieved).isNotBlank();
        // The transfer-limits chunk is the relevant one; its 10,000 figure must surface.
        assertThat(retrieved).contains("10,000");
    }

    @Test
    void retrievalDistinguishesTopics() {
        String feesAnswer = policyRetriever.retrieve("Is there a monthly maintenance fee?");

        assertThat(feesAnswer).isNotBlank();
        assertThat(feesAnswer.toLowerCase()).contains("fee");
    }

    @Test
    void chatReturnsNotConfiguredMessageWhenKeyIsBlank() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .with(jwt().jwt(builder -> builder.subject("user-42")))
                        .contentType("application/json")
                        .content("{\"message\":\"What is my balance?\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"reply\":\"" + AssistantService.NOT_CONFIGURED_MESSAGE + "\"}"));
    }

    @Test
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"What is my balance?\"}"))
                .andExpect(status().isUnauthorized());
    }
}
