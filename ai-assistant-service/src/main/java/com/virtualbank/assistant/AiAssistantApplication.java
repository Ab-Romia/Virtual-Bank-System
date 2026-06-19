package com.virtualbank.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The banking assistant. It answers a caller's questions about their own accounts
 * by calling read-only tools against the other services, and answers bank-policy
 * questions by retrieving the most relevant chunks of an embedded policy corpus.
 *
 * <p>It owns no business data: there is no JPA model, no outbox, and it consumes
 * no Kafka. Its only datastore is a pgvector table that holds the policy
 * embeddings, populated once on startup. Security, exception handling, and the
 * resource-server defaults arrive through vbank-common auto-configuration.
 */
@SpringBootApplication
public class AiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantApplication.class, args);
    }
}
