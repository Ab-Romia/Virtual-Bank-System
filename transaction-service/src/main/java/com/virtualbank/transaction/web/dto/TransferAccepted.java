package com.virtualbank.transaction.web.dto;

import com.virtualbank.transaction.domain.Transfer;

/**
 * The 202 response to POST /transfers. The statusUrl points at the GET endpoint
 * the caller polls to follow the transfer to its outcome.
 */
public record TransferAccepted(String transferId, String status, String statusUrl) {

    public static TransferAccepted of(Transfer transfer) {
        return new TransferAccepted(transfer.getId(), transfer.getStatus().name(),
                "/transfers/" + transfer.getId());
    }
}
