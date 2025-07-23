package com.virtualbank.transaction_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.transaction_service.dto.TransferInitiationRequest;
import com.virtualbank.transaction_service.dto.TransferInitiationResponse;
import com.virtualbank.transaction_service.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @WithMockUser(roles = "USER")
    public void testInitiateTransfer_Success() throws Exception {
        // Arrange - Fixed UUID format
        UUID fromAccountId = UUID.fromString("f1e2d3c4-b5a6-4876-9432-10fedcba9876");
        UUID toAccountId = UUID.fromString("a7b8c9d0-e1f2-3456-7890-abcdef123456");
        BigDecimal amount = new BigDecimal("30.00");

        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(amount);
        request.setDescription("Transfer to checking account");

        UUID transactionId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();

        TransferInitiationResponse response = TransferInitiationResponse.builder()
                .transactionId(transactionId)
                .status("INITIATED")
                .timestamp(now)
                .build();

        when(transactionService.initiateTransfer(any(TransferInitiationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/initiation")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInitiateTransfer_InvalidRequest_MissingFields() throws Exception {
        // Arrange
        TransferInitiationRequest request = new TransferInitiationRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/initiation")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInitiateTransfer_InvalidRequest_NegativeAmount() throws Exception {
        // Arrange - Fixed UUID format
        UUID fromAccountId = UUID.fromString("f1e2d3c4-b5a6-4876-9432-10fedcba9876");
        UUID toAccountId = UUID.fromString("a7b8c9d0-e1f2-3456-7890-abcdef123456");
        BigDecimal negativeAmount = new BigDecimal("-30.00");

        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(negativeAmount);
        request.setDescription("Transfer to checking account");

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/initiation")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}