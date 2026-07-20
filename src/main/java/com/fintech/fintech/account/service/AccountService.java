package com.fintech.fintech.account.service;

import com.fintech.fintech.account.dto.AccountDto;
import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.response.Response;

import java.util.List;

public interface AccountService {
    Account createAccount(AccountType accountType, User user);

    Response<List<AccountDto>> getMyAccounts();

    Response<?> closeAccount(String accountNumber);
}
