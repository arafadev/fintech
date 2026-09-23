package com.fintech.core.audit.service;

import com.fintech.core.account.dto.AccountDto;
import com.fintech.core.common.dto.UserDto;
import com.fintech.core.transaction.dto.TransactionDto;
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
