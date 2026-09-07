package com.fintech.core.account.service;

import com.fintech.core.account.dto.AccountDto;
import com.fintech.core.account.entity.Account;
import com.fintech.core.auth.entity.User;
import com.fintech.core.enums.AccountType;
import com.fintech.core.response.Response;

import java.util.List;

public interface AccountService {
    Account createAccount(AccountType accountType, User user);

    Response<List<AccountDto>> getMyAccounts();

    Response<?> closeAccount(String accountNumber);
}
