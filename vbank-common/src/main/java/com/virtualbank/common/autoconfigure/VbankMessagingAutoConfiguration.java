package com.virtualbank.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.common.outbox.OutboxAppender;
import com.virtualbank.common.outbox.OutboxRelay;
import com.virtualbank.common.outbox.OutboxRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wires the transactional outbox for services that opt in with
 * vbank.outbox.enabled=true and expose an OutboxRepository (account-service and
 * transaction-service). The appender records events in the producing
 * transaction; the relay forwards them to Kafka.
 */
@AutoConfiguration(after = {KafkaAutoConfiguration.class, JacksonAutoConfiguration.class,
        VbankCommonAutoConfiguration.class})
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "vbank.outbox", name = "enabled", havingValue = "true")
public class VbankMessagingAutoConfiguration {

    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    @ConditionalOnMissingBean
    public OutboxAppender outboxAppender(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        return new OutboxAppender(repository, objectMapper, clock);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class OutboxRelayConfiguration {

        @Bean
        @ConditionalOnBean({OutboxRepository.class, KafkaTemplate.class})
        @ConditionalOnMissingBean
        public OutboxRelay outboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka, Clock clock) {
            return new OutboxRelay(repository, kafka, clock);
        }
    }
}
