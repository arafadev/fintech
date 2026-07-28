package com.fintech.fintech.transaction.operation;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.enums.TransactionEntrySide;
import com.fintech.fintech.enums.TransactionType;
import com.fintech.fintech.exception.AccountNotFoundException;
import com.fintech.fintech.ledger.enums.LedgerEntryType;
import com.fintech.fintech.ledger.service.LedgerService;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.entity.Transaction;
import com.fintech.fintech.transaction.support.AccountTransactionGuard;
import com.fintech.fintech.transaction.support.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DepositOperation implements TransactionOperation {

    private final AccountRepository accountRepository;
    private final AccountTransactionGuard accountGuard;

    @Override
    public TransactionType supports() {
        return TransactionType.DEPOSIT;
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
        BigDecimal balanceBefore = Money.of(account.getBalance());
        BigDecimal balanceAfter = Money.add(balanceBefore, amount);

        account.setBalance(balanceAfter);
        accountRepository.save(account);

        transaction.setAccount(account);
        transaction.setEntrySide(TransactionEntrySide.CREDIT);
        transaction.setCorrelationId(request.getIdempotencyKey());

        ledgerDrafts.add(LedgerService.LedgerDraft.builder()
                .accountNumber(account.getAccountNumber())
                .signedAmount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .entryType(LedgerEntryType.CREDIT)
                .build());

        return null;
    }
}
