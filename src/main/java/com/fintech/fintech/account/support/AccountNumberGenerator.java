package com.fintech.fintech.account.support;

import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final AppProperties appProperties;
    private final SecureRandom random = new SecureRandom();

    public String nextUnique() {
        String prefix = appProperties.getAccount().getNumberPrefix();
        int length = appProperties.getAccount().getNumberLength();
        int bound = (int) Math.pow(10, length);
        int min = (int) Math.pow(10, length - 1);

        String accountNumber;
        do {
            accountNumber = prefix + (random.nextInt(bound - min) + min);
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }
}
