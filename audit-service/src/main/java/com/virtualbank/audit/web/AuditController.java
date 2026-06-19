package com.virtualbank.audit.web;

import com.virtualbank.audit.AuditService;
import com.virtualbank.audit.web.dto.AuditEntryView;
import com.virtualbank.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Returns the ordered audit history of a transfer to the initiator who started
     * it. The caller is the authenticated user, so one user cannot read another's
     * history.
     */
    @GetMapping("/transfers/{transferId}")
    public List<AuditEntryView> history(@PathVariable String transferId) {
        return auditService.historyFor(transferId, CurrentUser.requireId());
    }
}
