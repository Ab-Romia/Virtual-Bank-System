package com.virtualbank.bff_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "logging-topic";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendLog(Object logMessage) {
        this.kafkaTemplate.send(TOPIC, logMessage);
        System.out.println("Sent log message to Kafka: " + logMessage);
    }
}