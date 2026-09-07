package com.fintech.core.transaction.entity;

import com.fintech.core.account.entity.Account;
import com.fintech.core.enums.TransactionEntrySide;
import com.fintech.core.enums.TransactionStatus;
import com.fintech.core.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_account_date", columnList = "account_id, transaction_date"),
                @Index(name = "idx_txn_dest_date", columnList = "destination_account, transaction_date"),
                @Index(name = "idx_txn_idempotency", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_txn_type_status_date", columnList = "transaction_type, status, transaction_date")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    private String description;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_side", length = 10)
    private TransactionEntrySide entrySide;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    // for transfer
    @Column(name = "source_account")
    private String sourceAccount;

    @Column(name = "destination_account")
    private String destinationAccount;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
}
