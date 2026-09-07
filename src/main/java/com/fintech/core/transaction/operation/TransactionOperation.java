package com.fintech.core.transaction.operation;

import com.fintech.core.enums.TransactionType;
import com.fintech.core.ledger.service.LedgerService;
import com.fintech.core.transaction.dto.TransactionRequest;
import com.fintech.core.transaction.entity.Transaction;

import java.util.List;

public interface TransactionOperation {

    TransactionType supports();

    Transaction execute(
            TransactionRequest request,
            Transaction primary,
            List<LedgerService.LedgerDraft> ledgerDrafts
    );
}
