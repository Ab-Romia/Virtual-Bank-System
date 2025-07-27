package com.virtualbank.logging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.logging.dto.LogMessage;
import com.virtualbank.logging.entity.LogEntry;
import com.virtualbank.logging.repository.LogEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;

@Service
@Slf4j
public class KafkaConsumerService {

    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaConsumerService(LogEntryRepository logEntryRepository, ObjectMapper objectMapper) {
        this.logEntryRepository = logEntryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${logging.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listen(HashMap<String, Object> messageMap) {
        try {
            log.info("Received message from Kafka: {}", messageMap);

            String messageStr = objectMapper.writeValueAsString(messageMap.get("message"));

            OffsetDateTime dateTime;
            Object rawDateTime = messageMap.get("dateTime");
            if (rawDateTime instanceof Number) {
                dateTime = OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(((Number) rawDateTime).longValue()),
                        ZoneId.systemDefault()
                );
            } else {
                dateTime = objectMapper.convertValue(rawDateTime, OffsetDateTime.class);
            }

            LogEntry logEntry = LogEntry.builder()
                    .message(messageStr)
                    .messageType((String) messageMap.get("messageType"))
                    .dateTime(dateTime)
                    .sourceService((String) messageMap.get("sourceService"))
                    .sourceEndpoint((String) messageMap.get("sourceEndpoint"))
                    .appName((String) messageMap.get("appName"))
                    .createdAt(OffsetDateTime.now())  // Set createdAt explicitly
                    .build();

            LogEntry savedEntry = logEntryRepository.save(logEntry);
            log.info("Saved log entry to the database with ID: {}", savedEntry.getId());

        } catch (Exception e) {
            log.error("An unexpected error occurred while processing the message: {}", messageMap, e);
            e.printStackTrace();
        }
    }
}