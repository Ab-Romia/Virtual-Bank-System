package com.virtualbank.logging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogMessage {
    private Object message;
    private String messageType;
    private OffsetDateTime dateTime;
    private String sourceService;
    private String sourceEndpoint;
    private String appName;
}