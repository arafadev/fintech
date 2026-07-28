package com.fintech.fintech.audit.service;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.common.dto.UserDto;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.transaction.dto.TransactionDto;
import com.fintech.fintech.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditorServiceImpl implements AuditorService {

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final ModelMapper modelMapper;

    @Override
    public Map<String, Long> getSystemTotals() {
        long totalUsers = userRepo.count();
        long totalAccounts = accountRepo.count();
        long totalTransactions = transactionRepo.count();

        return Map.of(
                "totalUsers", totalUsers,
                "totalAccounts", totalAccounts,
                "totalTransactions", totalTransactions
        );
    }

    @Override
    public Optional<UserDto> findUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .map(user -> modelMapper.map(user, UserDto.class));
    }

    @Override
    public Optional<AccountDto> findAccountDetailsByAccountNumber(String accountNumber) {
        return accountRepo.findByAccountNumber(accountNumber)
                .map(account -> modelMapper.map(account, AccountDto.class));
    }

    @Override
    public Page<TransactionDto> findTransactionsByAccountNumber(String accountNumber, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return transactionRepo.findByAccount_AccountNumber(accountNumber, pageable)
                .map(transaction -> modelMapper.map(transaction, TransactionDto.class));
    }

    @Override
    public Optional<TransactionDto> findTransactionById(Long transactionId) {
        return transactionRepo.findById(transactionId)
                .map(transaction -> modelMapper.map(transaction, TransactionDto.class));
    }
}
