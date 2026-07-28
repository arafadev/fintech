package com.fintech.fintech.account.service.impl;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.account.support.AccountNumberGenerator;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.enums.AccountStatus;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.enums.Currency;
import com.fintech.fintech.exception.AccountNotFoundException;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Single responsibility: account lifecycle (create / list mine / close).
 * Number generation is delegated to {@link AccountNumberGenerator}.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Messages messages;

    @Override
    public Account createAccount(AccountType accountType, User user) {
        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.nextUnique())
                .accountType(accountType)
                .currency(Currency.USD)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    @Override
    public Response<List<AccountDto>> getMyAccounts() {
        User user = userService.getCurrentLoggedInUser();

        List<AccountDto> accounts = accountRepository.findByUserId(user.getId())
                .stream()
                .map(account -> modelMapper.map(account, AccountDto.class))
                .toList();

        return Response.ok(messages.get("success.accounts_fetched"), accounts);
    }

    @Override
    public Response<?> closeAccount(String accountNumber) {
        User user = userService.getCurrentLoggedInUser();
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (!user.getAccounts().contains(account)) {
            throw new NotFoundException("error.account.not_belong_you");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("error.account.balance_must_be_zero");
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);

        return Response.ok(messages.get("success.account_closed"));
    }
}
