package com.virtualbank.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.account.dto.TransferRequest;
import com.virtualbank.account.model.Account;
import com.virtualbank.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID accountId1;
    private UUID accountId2;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        accountRepository.deleteAll();

        // Create test accounts
        accountId1 = UUID.fromString("4958f017-b452-41b8-be6b-6568e118db25");
        accountId2 = UUID.fromString("ca3de2fb-8cf7-42d8-9eb2-cbe4f522a3d6");

        Account account1 = new Account();
        account1.setAccountId(accountId1);
        account1.setUserId(UUID.randomUUID());
        account1.setAccountNumber("1234567890");
        account1.setAccountType("SAVINGS");
        account1.setBalance(new BigDecimal("1000.00"));
        account1.setStatus("ACTIVE");
        account1.setCreatedAt(ZonedDateTime.now());
        account1.setUpdatedAt(ZonedDateTime.now());

        Account account2 = new Account();
        account2.setAccountId(accountId2);
        account2.setUserId(UUID.randomUUID());
        account2.setAccountNumber("0987654321");
        account2.setAccountType("CHECKING");
        account2.setBalance(new BigDecimal("500.00"));
        account2.setStatus("ACTIVE");
        account2.setCreatedAt(ZonedDateTime.now());
        account2.setUpdatedAt(ZonedDateTime.now());

        accountRepository.save(account1);
        accountRepository.save(account2);
    }

    @Test
    @WithMockUser
    void getAccount_Success() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", accountId1.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId1.toString()))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value("1000.0"));
    }

    @Test
    @WithMockUser
    void transferFunds_Success() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(accountId1);
        request.setToAccountId(accountId2);
        request.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify balances updated
        mockMvc.perform(get("/accounts/{accountId}", accountId1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("900.0"));

        mockMvc.perform(get("/accounts/{accountId}", accountId2.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("600.0"));
    }

    @Test
    void transferFunds_InsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(accountId1);
        request.setToAccountId(accountId2);
        request.setAmount(new BigDecimal("2000.00")); // More than available balance

        mockMvc.perform(put("/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getAccount_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/accounts/{accountId}", nonExistentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}