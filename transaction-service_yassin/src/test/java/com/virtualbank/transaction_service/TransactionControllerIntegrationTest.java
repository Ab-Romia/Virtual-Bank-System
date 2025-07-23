package com.virtualbank.transaction_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.transaction_service.dto.TransferInitiationRequest;
import com.virtualbank.transaction_service.dto.TransferInitiationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER") // Add mock authentication
    public void testInitiateTransfer_EndToEnd() throws Exception {
        // Arrange
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("30.00");

        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(amount);
        request.setDescription("Transfer to checking account");

        // Act - Add CSRF token
        MvcResult result = mockMvc.perform(post("/transactions/transfer/initiation")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        TransferInitiationResponse response = objectMapper.readValue(responseJson, TransferInitiationResponse.class);

        assertNotNull(response.getTransactionId());
        assertEquals("INITIATED", response.getStatus());
        assertNotNull(response.getTimestamp());
    }
}