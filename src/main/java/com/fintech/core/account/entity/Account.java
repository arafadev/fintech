package com.fintech.core.account.entity;

import com.fintech.core.auth.entity.User;
import com.fintech.core.enums.AccountStatus;
import com.fintech.core.enums.AccountType;
import com.fintech.core.enums.Currency;
import com.fintech.core.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_user", columnList = "user_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( name = "account_number", nullable = false, unique = true, length = 15)
    private String accountNumber;

    @Column(name = "balance" , precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;


    @Version
    private Long version;

    @Column(name = "account_type", nullable=false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transaction = new ArrayList<>();


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
