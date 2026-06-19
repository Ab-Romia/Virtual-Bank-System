package com.virtualbank.account.config;

import com.virtualbank.common.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka wiring for the transfer command listener: the topics this service relies
 * on (the broker does not auto-create them) and a listener factory whose error
 * handler retries a failing record a couple of times and then publishes it to the
 * dead-letter topic so a poison message cannot stall the partition.
 *
 * <p>Gated on a KafkaTemplate so the whole configuration backs off when Kafka
 * autoconfiguration is excluded, as it is in the broker-free tests.
 */
@Configuration
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaConfig {

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

    @Bean
    public DefaultErrorHandler transferErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler transferErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(transferErrorHandler);
        return factory;
    }
}
