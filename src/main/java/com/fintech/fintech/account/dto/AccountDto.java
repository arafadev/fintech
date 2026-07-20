package com.fintech.fintech.account.dto;

import com.fasterxml.jackson.annotation.*;
import com.fintech.fintech.auth.dto.UserDto;
import com.fintech.fintech.enums.AccountStatus;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.enums.Currency;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.transaction.dto.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private Long id;

    private String accountNumber;

    private BigDecimal balance;

    private AccountType accountType;

    @JsonBackReference// this will not be added to the account dto. It will be ignored because it is a back refrence
    private UserDto user;

    private Currency currency;

    private AccountStatus status;

    @JsonManagedReference// if helps avoid recursion loop by ignoring the AccountDTO withing the TransactionDTO
    private List<TransactionDto> transactions;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
