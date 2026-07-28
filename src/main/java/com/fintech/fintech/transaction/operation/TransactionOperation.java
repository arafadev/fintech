package com.fintech.fintech.transaction.operation;

import com.fintech.fintech.enums.TransactionType;
import com.fintech.fintech.ledger.service.LedgerService;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.entity.Transaction;

import java.util.List;

public interface TransactionOperation {

    TransactionType supports();

    Transaction execute(
            TransactionRequest request,
            Transaction primary,
            List<LedgerService.LedgerDraft> ledgerDrafts
    );
}
