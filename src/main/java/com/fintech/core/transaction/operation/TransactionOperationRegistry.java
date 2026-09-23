package com.fintech.core.transaction.operation;

import com.fintech.core.enums.TransactionType;
import com.fintech.core.exception.InvalidTransactionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionOperationRegistry {

    private final Map<TransactionType, TransactionOperation> byType;

    public TransactionOperationRegistry(List<TransactionOperation> operations) {
        Map<TransactionType, TransactionOperation> map = new EnumMap<>(TransactionType.class);
        for (TransactionOperation operation : operations) {
            TransactionOperation previous = map.put(operation.supports(), operation);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate TransactionOperation for type: " + operation.supports());
            }
        }
        this.byType = Map.copyOf(map);
    }

    public TransactionOperation require(TransactionType type) {
        TransactionOperation operation = byType.get(type);
        if (operation == null) {
            throw new InvalidTransactionException("error.transaction.invalid_type");
        }
        return operation;
    }
}
