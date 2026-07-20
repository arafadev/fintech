package com.fintech.fintech.audit.service;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.auth.dto.UserDto;
import com.fintech.fintech.transaction.dto.TransactionDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditorService {

    Map<String, Long> getSystemTotals();

    Optional<UserDto> findUserByEmail(String email);

    Optional<AccountDto> findAccountDetailsByAccountNumber(String accountNumber);

    List<TransactionDto> findTransactionsByAccountNumber(String accountNumber);

    Optional<TransactionDto> findTransactionById(Long transactionId);
}