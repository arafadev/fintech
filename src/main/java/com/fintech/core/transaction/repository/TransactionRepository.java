package com.fintech.core.transaction.repository;

import com.fintech.core.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber " +
            "OR (t.transactionType = 'TRANSFER' AND t.destinationAccount = :accountNumber) " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccount_AccountNumber(String accountNumber, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.account.accountNumber = :accountNumber
              AND t.status = 'SUCCESS'
              AND t.transactionDate >= :since
              AND (
                    t.transactionType = 'WITHDRAWAL'
                 OR (t.transactionType = 'TRANSFER' AND t.entrySide = 'DEBIT')
              )
            """)
    BigDecimal sumSuccessfulOutgoingSince(
            @Param("accountNumber") String accountNumber,
            @Param("since") LocalDateTime since
    );
}
