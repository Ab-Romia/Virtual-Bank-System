package com.virtualbank.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.common.audit.AuditPublisher;
import com.virtualbank.common.outbox.OutboxAppender;
import com.virtualbank.common.outbox.OutboxRelay;
import com.virtualbank.common.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
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
 * Wires the Kafka audit publisher and the transactional outbox. The audit
 * publisher appears wherever a KafkaTemplate exists, so a service without a
 * broker (or with Kafka excluded in a test) simply does not get one. The outbox
 * beans appear only when a service opts in with vbank.outbox.enabled=true and
 * exposes an OutboxRepository (account-service and transaction-service).
 */
@AutoConfiguration(after = {KafkaAutoConfiguration.class, JacksonAutoConfiguration.class,
        VbankCommonAutoConfiguration.class})
@ConditionalOnClass(KafkaTemplate.class)
public class VbankMessagingAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    public AuditPublisher auditPublisher(KafkaTemplate<String, String> kafka,
                                         ObjectMapper objectMapper,
                                         @Value("${spring.application.name:unknown}") String serviceName,
                                         Clock clock) {
        return new AuditPublisher(kafka, objectMapper, serviceName, clock);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "vbank.outbox", name = "enabled", havingValue = "true")
    @EnableScheduling
    static class OutboxConfiguration {

        @Bean
        @ConditionalOnBean(OutboxRepository.class)
        @ConditionalOnMissingBean
        public OutboxAppender outboxAppender(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
            return new OutboxAppender(repository, objectMapper, clock);
        }

        @Bean
        @ConditionalOnBean({OutboxRepository.class, KafkaTemplate.class})
        @ConditionalOnMissingBean
        public OutboxRelay outboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka, Clock clock) {
            return new OutboxRelay(repository, kafka, clock);
        }
    }
}
