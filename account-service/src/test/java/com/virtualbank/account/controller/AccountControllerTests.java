package com.virtualbank.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.account.dto.AccountResponse;
import com.virtualbank.account.dto.TransferRequest;
import com.virtualbank.account.dto.TransferResponse;
import com.virtualbank.account.exception.AccountNotFoundException;
import com.virtualbank.account.exception.InsufficientFundsException;
import com.virtualbank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
public class AccountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID accountId1;
    private UUID accountId2;
    private AccountResponse mockAccountResponse;
    private TransferRequest validTransferRequest;

    @BeforeEach
    void setUp() {
        accountId1 = UUID.fromString("4958f017-b452-41b8-be6b-6568e118db25");
        accountId2 = UUID.fromString("ca3de2fb-8cf7-42d8-9eb2-cbe4f522a3d6");

        // Setup mock account response
        mockAccountResponse = new AccountResponse();
        mockAccountResponse.setAccountId(accountId1);
        mockAccountResponse.setUserId(UUID.randomUUID());
        mockAccountResponse.setAccountNumber("1234567890");
        mockAccountResponse.setAccountType("SAVINGS");
        mockAccountResponse.setBalance(new BigDecimal("1000.00"));
        mockAccountResponse.setStatus("ACTIVE");
        mockAccountResponse.setCreatedAt(ZonedDateTime.now());
        mockAccountResponse.setUpdatedAt(ZonedDateTime.now());

        // Setup valid transfer request
        validTransferRequest = new TransferRequest();
        validTransferRequest.setFromAccountId(accountId1);
        validTransferRequest.setToAccountId(accountId2);
        validTransferRequest.setAmount(new BigDecimal("100.00"));
    }

    @Test
    @WithMockUser
    void getAccount_Success() throws Exception {
        when(accountService.getAccount(accountId1)).thenReturn(mockAccountResponse);

        mockMvc.perform(get("/accounts/{accountId}", accountId1.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId1.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));

        verify(accountService, times(1)).getAccount(accountId1);
    }

    @Test
    @WithMockUser
    void getAccount_NotFound() throws Exception {
        when(accountService.getAccount(accountId1))
                .thenThrow(new AccountNotFoundException("Account with ID " + accountId1 + " not found"));

        mockMvc.perform(get("/accounts/{accountId}", accountId1.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).getAccount(accountId1);
    }

    @Test
    @WithMockUser
    void getAccount_InvalidUuid() throws Exception {
        String invalidUuid = "not-a-uuid";

        mockMvc.perform(get("/accounts/{accountId}", invalidUuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(accountService, never()).getAccount(any(UUID.class));
    }

    @Test
    @WithMockUser
    void transferFunds_Success() throws Exception {
        TransferResponse mockResponse = new TransferResponse("Transfer completed successfully");

        when(accountService.transferFunds(any(TransferRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"));

        verify(accountService, times(1)).transferFunds(any(TransferRequest.class));
    }

    @Test
    @WithMockUser
    void transferFunds_SourceAccountNotFound() throws Exception {
        when(accountService.transferFunds(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Account with ID " + accountId1 + " not found"));

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).transferFunds(any(TransferRequest.class));
    }

    @Test
    @WithMockUser
    void transferFunds_InsufficientFunds() throws Exception {
        when(accountService.transferFunds(any(TransferRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds in account " + accountId1));

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, times(1)).transferFunds(any(TransferRequest.class));
    }
}