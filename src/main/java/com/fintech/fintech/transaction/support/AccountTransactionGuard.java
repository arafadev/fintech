package com.fintech.fintech.transaction.support;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.enums.AccountStatus;
import com.fintech.fintech.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountTransactionGuard {

    private final UserService userService;

    public void assertOwnership(Account account) {
        var current = userService.getCurrentLoggedInUser();
        if (!account.getUser().getId().equals(current.getId())) {
            throw new BadRequestException("error.account.not_belong");
        }
    }

    public void assertOperable(Account account) {
        AccountStatus status = account.getStatus();

        if (status == AccountStatus.CLOSED) {
            throw new BadRequestException("error.account.closed");
        }
        if (status == AccountStatus.SUSPENDED) {
            throw new BadRequestException("error.account.suspended");
        }
        if (status != AccountStatus.ACTIVE) {
            throw new BadRequestException("error.account.not_active", status);
        }
    }

    public void assertOwnedAndOperable(Account account) {
        assertOperable(account);
        assertOwnership(account);
    }
}
