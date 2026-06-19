package com.virtualbank.transaction.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.virtualbank.common.messaging.Topics;

/**
 * Wires the listener container factory used by {@link TransferEventListener}. A
 * message that keeps failing is retried twice a second apart and then routed to
 * transfer.events.DLT by the dead-letter recoverer rather than blocking the
 * partition. Active only when a KafkaTemplate exists, so tests that exclude Kafka
 * auto-configuration skip it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaListenerConfig {

    @Bean
    public DefaultErrorHandler transferEventErrorHandler(KafkaTemplate<String, String> kafka) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, exception) -> deadLetterFor(record));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    private TopicPartition deadLetterFor(ConsumerRecord<?, ?> record) {
        String dlt = record.topic().equals(Topics.TRANSFER_EVENTS)
                ? Topics.TRANSFER_EVENTS_DLT
                : record.topic() + ".DLT";
        return new TopicPartition(dlt, record.partition());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler transferEventErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(transferEventErrorHandler);
        return factory;
    }
}
