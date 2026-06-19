package com.virtualbank.transaction.web;

import com.virtualbank.common.security.CurrentUser;
import com.virtualbank.transaction.TransferService;
import com.virtualbank.transaction.domain.Transfer;
import com.virtualbank.transaction.web.dto.TransferAccepted;
import com.virtualbank.transaction.web.dto.TransferRequest;
import com.virtualbank.transaction.web.dto.TransferView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Accepts a transfer and returns 202 with a status URL to poll. The initiator
     * is always the authenticated user, never a request field. The Idempotency-Key
     * header makes a retried submission resolve to the original transfer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransferAccepted create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                   @Valid @RequestBody TransferRequest request) {
        Transfer transfer = transferService.requestTransfer(CurrentUser.requireId(), idempotencyKey, request);
        return TransferAccepted.of(transfer);
    }

    @GetMapping("/{id}")
    public TransferView byId(@PathVariable String id) {
        return TransferView.of(transferService.getForInitiator(id, CurrentUser.requireId()));
    }

    @GetMapping
    public List<TransferView> list() {
        return transferService.listForInitiator(CurrentUser.requireId()).stream()
                .map(TransferView::of)
                .toList();
    }
}
