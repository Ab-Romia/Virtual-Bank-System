package com.virtualbank.transaction_service.service;

import com.virtualbank.transaction_service.dto.TransferInitiationRequest;
import com.virtualbank.transaction_service.dto.TransferInitiationResponse;
import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import com.virtualbank.transaction_service.entity.TransactionType;
import com.virtualbank.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    public void testInitiateTransfer_Success() {
        // Arrange - Fixed UUID format
        UUID fromAccountId = UUID.fromString("f1e2d3c4-b5a6-4876-9432-10fedcba9876");
        UUID toAccountId = UUID.fromString("a7b8c9d0-e1f2-3456-7890-abcdef123456");
        BigDecimal amount = new BigDecimal("30.00");
        String description = "Transfer to checking account";

        TransferInitiationRequest request = new TransferInitiationRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(amount);
        request.setDescription(description);

        UUID transactionId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();

        // Create a complete transaction object to be returned
        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(transactionId);
        savedTransaction.setFromAccountId(fromAccountId);
        savedTransaction.setToAccountId(toAccountId);
        savedTransaction.setAmount(amount);
        savedTransaction.setDescription(description);
        savedTransaction.setStatus(TransactionStatus.INITIATED);
        savedTransaction.setTransactionType(TransactionType.TRANSFER);
        savedTransaction.setCreatedAt(now);
        savedTransaction.setUpdatedAt(now);

        // Mock repository to return our complete transaction
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransferInitiationResponse response = transactionService.initiateTransfer(request);

        // Assert
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertEquals(fromAccountId, capturedTransaction.getFromAccountId());
        assertEquals(toAccountId, capturedTransaction.getToAccountId());
        assertEquals(amount, capturedTransaction.getAmount());
        assertEquals(description, capturedTransaction.getDescription());
        assertEquals(TransactionStatus.INITIATED, capturedTransaction.getStatus());
        assertEquals(TransactionType.TRANSFER, capturedTransaction.getTransactionType());

        // These will only pass if the service correctly builds and returns a response
        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
        assertEquals("INITIATED", response.getStatus());
        assertNotNull(response.getTimestamp());
    }
}