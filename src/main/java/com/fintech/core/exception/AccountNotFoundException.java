package com.fintech.core.exception;

public class AccountNotFoundException extends NotFoundException {

    public AccountNotFoundException(String accountNumber) {
        super("error.account.not_found_with_number", accountNumber);
    }

    public AccountNotFoundException() {
        super("error.account.not_found");
    }
}
