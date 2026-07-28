package com.fintech.fintech.audit.service;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.common.dto.UserDto;
import com.fintech.fintech.transaction.dto.TransactionDto;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Optional;

public interface AuditorService {

    Map<String, Long> getSystemTotals();

    Optional<UserDto> findUserByEmail(String email);

    Optional<AccountDto> findAccountDetailsByAccountNumber(String accountNumber);

    Page<TransactionDto> findTransactionsByAccountNumber(String accountNumber, int page, int size);

    Optional<TransactionDto> findTransactionById(Long transactionId);
}
