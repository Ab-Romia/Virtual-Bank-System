package com.virtualbank.transaction.messaging;

import com.virtualbank.common.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the transfer topics and their dead-letter topics so they exist with a
 * single partition and replication factor one, which suits the single-broker
 * development setup. Topic auto-creation is off on the broker, so these beans are
 * how the topics come to exist. Skipped when no broker is configured (for example
 * in tests that exclude Kafka auto-configuration).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class KafkaTopicConfig {

    @Bean
    public NewTopic transferCommandsTopic() {
        return TopicBuilder.name(Topics.TRANSFER_COMMANDS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferEventsTopic() {
        return TopicBuilder.name(Topics.TRANSFER_EVENTS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferCommandsDltTopic() {
        return TopicBuilder.name(Topics.TRANSFER_COMMANDS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferEventsDltTopic() {
        return TopicBuilder.name(Topics.TRANSFER_EVENTS_DLT).partitions(1).replicas(1).build();
    }
}
