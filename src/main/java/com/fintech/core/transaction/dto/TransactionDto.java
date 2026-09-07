package com.fintech.core.transaction.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fintech.core.account.dto.AccountDto;
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

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long id;

    private BigDecimal amount;

    private TransactionType transactionType;

    private LocalDateTime transactionDate;

    private String description;

    private TransactionStatus status;

    private TransactionEntrySide entrySide;
    private String correlationId;

    @JsonBackReference
    private AccountDto account;

    //for transfer
    private String sourceAccount;
    private String destinationAccount;

    private String idempotencyKey;

}
