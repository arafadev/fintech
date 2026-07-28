package com.fintech.fintech.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fintech.fintech.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    @NotNull(message = "{validation.transaction_type.required}")
    private TransactionType transactionType;

    @NotNull(message = "{validation.amount.required}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{validation.amount.min}")
    @Digits(integer = 17, fraction = 2, message = "{validation.amount.digits}")
    private BigDecimal amount;

    @NotBlank(message = "{validation.account_number.required}")
    @Size(min = 10, max = 15, message = "{validation.account_number.size}")
    private String accountNumber;

    @Size(max = 255, message = "{validation.description.size}")
    private String description;

    @Size(min = 10, max = 15, message = "{validation.destination_account.size}")
    private String destinationAccountNumber;

    @NotBlank(message = "{validation.idempotency.required}")
    @Size(max = 64, message = "{validation.idempotency.size}")
    private String idempotencyKey;
}
