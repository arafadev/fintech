package com.fintech.core.transaction.operation;

import com.fintech.core.account.entity.Account;
import com.fintech.core.account.repository.AccountRepository;
import com.fintech.core.enums.TransactionEntrySide;
import com.fintech.core.enums.TransactionType;
import com.fintech.core.exception.AccountNotFoundException;
import com.fintech.core.exception.InsufficientBalanceException;
import com.fintech.core.ledger.enums.LedgerEntryType;
import com.fintech.core.ledger.service.LedgerService;
import com.fintech.core.transaction.dto.TransactionRequest;
import com.fintech.core.transaction.entity.Transaction;
import com.fintech.core.transaction.support.AccountTransactionGuard;
import com.fintech.core.transaction.support.Money;
import com.fintech.core.transaction.support.TransactionLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WithdrawalOperation implements TransactionOperation {

    private final AccountRepository accountRepository;
    private final AccountTransactionGuard accountGuard;
    private final TransactionLimitService limitService;

    @Override
    public TransactionType supports() {
        return TransactionType.WITHDRAWAL;
    }

    @Override
    public Transaction execute(
            TransactionRequest request,
            Transaction transaction,
            List<LedgerService.LedgerDraft> ledgerDrafts
    ) {
        Account account = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountNumber()));

        accountGuard.assertOwnedAndOperable(account);

        BigDecimal amount = Money.of(request.getAmount());
        limitService.assertOutgoingAllowed(account, amount);

        BigDecimal balanceBefore = Money.of(account.getBalance());
        if (balanceBefore.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("error.balance.insufficient");
        }

        BigDecimal balanceAfter = Money.subtract(balanceBefore, amount);
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        transaction.setAccount(account);
        transaction.setEntrySide(TransactionEntrySide.DEBIT);
        transaction.setCorrelationId(request.getIdempotencyKey());

        ledgerDrafts.add(LedgerService.LedgerDraft.builder()
                .accountNumber(account.getAccountNumber())
                .signedAmount(amount.negate())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .entryType(LedgerEntryType.DEBIT)
                .build());

        return null;
    }
}
