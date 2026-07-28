package com.fintech.fintech.transaction.controller;

import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/api/transactions")
    public ResponseEntity<Response<?>> createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody TransactionRequest request
    ) {
        if (StringUtils.hasText(idempotencyKeyHeader)) {
            request.setIdempotencyKey(idempotencyKeyHeader.trim());
        }
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }

    @GetMapping("/api/accounts/{accountNumber}/transactions")
    public ResponseEntity<Response<?>> getTransactionsForMyAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(transactionService.getTransactionsForMyAccount(accountNumber, page, size));
    }
}
