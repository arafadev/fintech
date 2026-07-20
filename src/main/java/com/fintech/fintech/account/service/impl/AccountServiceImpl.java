package com.fintech.fintech.account.service.impl;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.enums.AccountStatus;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.enums.Currency;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    private final Random random = new Random();

    @Override
    public Account createAccount(AccountType accountType, User user) {
        log.info("Insdie createAccount()");

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
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

        return Response.<List<AccountDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User accounts fetched successfully")
                .data(accounts)
                .build();
    }

    @Override
    public Response<?> closeAccount(String accountNumber) {

        User user = userService.getCurrentLoggedInUser();
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account Not Found"));

        if (!user.getAccounts().contains(account)) {
            throw new NotFoundException("Account doesn't belong to you");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Account balance must be zero before closing");
        }
        account.setStatus(AccountStatus.CLOSED);
//        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account closed successfully")
                .build();
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate a random 8-digit number (from 10,000,000 to 99,999,999)
            // and combine it with the "66" prefix.
            accountNumber = "66" + (random.nextInt(90000000) + 10000000);

        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());


        log.info("account number generated {}", accountRepository);
        return accountNumber;
    }

}
