package com.fintech.fintech.audit.controller;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.audit.service.AuditorService;
import com.fintech.fintech.common.dto.UserDto;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only supervisory API — resource-oriented paths under {@code /api/audit}.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditorController {

    private final AuditorService auditorService;
    private final Messages messages;

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
    @GetMapping("/totals")
    public ResponseEntity<Response<Map<String, Long>>> getSystemTotals() {
        return ResponseEntity.ok(Response.ok(messages.get("success.system_totals"), auditorService.getSystemTotals()));
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
    @GetMapping("/users")
    public ResponseEntity<Response<UserDto>> findUserByEmail(@RequestParam String email) {
        UserDto user = auditorService.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("error.auth.user_not_found"));
        return ResponseEntity.ok(Response.ok(messages.get("success.user_retrieved"), user));
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Response<AccountDto>> findAccountDetailsByAccountNumber(
            @PathVariable String accountNumber) {
        AccountDto account = auditorService.findAccountDetailsByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("error.account.not_found"));
        return ResponseEntity.ok(Response.ok(messages.get("success.account_retrieved"), account));
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
    @GetMapping("/accounts/{accountNumber}/transactions")
    public ResponseEntity<Response<List<TransactionDto>>> getTransactionsByAccountNumber(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<TransactionDto> txns = auditorService.findTransactionsByAccountNumber(accountNumber, page, size);

        Map<String, Serializable> meta = new HashMap<>();
        meta.put("currentPage", txns.getNumber());
        meta.put("totalItems", txns.getTotalElements());
        meta.put("totalPages", txns.getTotalPages());
        meta.put("pageSize", txns.getSize());

        return ResponseEntity.ok(Response.ok(messages.get("success.transactions_retrieved"), txns.getContent(), meta));
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
    @GetMapping("/transactions/{id}")
    public ResponseEntity<Response<TransactionDto>> getTransactionById(@PathVariable Long id) {
        TransactionDto txn = auditorService.findTransactionById(id)
                .orElseThrow(() -> new NotFoundException("error.transaction.not_found"));
        return ResponseEntity.ok(Response.ok(messages.get("success.transaction_retrieved"), txn));
    }
}
