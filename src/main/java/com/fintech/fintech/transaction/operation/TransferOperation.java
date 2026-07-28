package com.fintech.fintech.transaction.operation;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.enums.TransactionEntrySide;
import com.fintech.fintech.enums.TransactionType;
import com.fintech.fintech.exception.AccountNotFoundException;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.InsufficientBalanceException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.ledger.enums.LedgerEntryType;
import com.fintech.fintech.ledger.service.LedgerService;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.entity.Transaction;
import com.fintech.fintech.transaction.support.AccountTransactionGuard;
import com.fintech.fintech.transaction.support.Money;
import com.fintech.fintech.transaction.support.TransactionLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
@RequiredArgsConstructor
public class TransferOperation implements TransactionOperation {

    private final AccountRepository accountRepository;
    private final AccountTransactionGuard accountGuard;
    private final TransactionLimitService limitService;

    @Override
    public TransactionType supports() {
        return TransactionType.TRANSFER;
    }

    @Override
    public Transaction execute(
            TransactionRequest request,
            Transaction debitTransaction,
            List<LedgerService.LedgerDraft> ledgerDrafts
    ) {
        String sourceNumber = request.getAccountNumber();
        String destNumber = request.getDestinationAccountNumber();

        if (sourceNumber.equals(destNumber)) {
            throw new BadRequestException("error.transaction.same_account");
        }

        AccountPair locked = lockInStableOrder(sourceNumber, destNumber);
        Account sourceAccount = locked.source();
        Account destination = locked.destination();

        accountGuard.assertOperable(sourceAccount);
        accountGuard.assertOperable(destination);
        accountGuard.assertOwnership(sourceAccount);

        BigDecimal amount = Money.of(request.getAmount());
        BigDecimal sourceBefore = Money.of(sourceAccount.getBalance());
        BigDecimal destBefore = Money.of(destination.getBalance());

        limitService.assertOutgoingAllowed(sourceAccount, amount);

        if (sourceBefore.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("error.balance.insufficient_source");
        }

        BigDecimal sourceAfter = Money.subtract(sourceBefore, amount);
        BigDecimal destAfter = Money.add(destBefore, amount);

        sourceAccount.setBalance(sourceAfter);
        accountRepository.save(sourceAccount);
        destination.setBalance(destAfter);
        accountRepository.save(destination);

        String correlationId = request.getIdempotencyKey();

        debitTransaction.setAccount(sourceAccount);
        debitTransaction.setSourceAccount(sourceAccount.getAccountNumber());
        debitTransaction.setDestinationAccount(destination.getAccountNumber());
        debitTransaction.setEntrySide(TransactionEntrySide.DEBIT);
        debitTransaction.setCorrelationId(correlationId);

        Transaction creditTransaction = new Transaction();
        creditTransaction.setTransactionType(TransactionType.TRANSFER);
        creditTransaction.setAmount(amount);
        creditTransaction.setDescription(request.getDescription());
        creditTransaction.setAccount(destination);
        creditTransaction.setSourceAccount(sourceAccount.getAccountNumber());
        creditTransaction.setDestinationAccount(destination.getAccountNumber());
        creditTransaction.setEntrySide(TransactionEntrySide.CREDIT);
        creditTransaction.setCorrelationId(correlationId);
        creditTransaction.setIdempotencyKey(correlationId + ":CR");

        ledgerDrafts.add(LedgerService.LedgerDraft.builder()
                .accountNumber(sourceAccount.getAccountNumber())
                .signedAmount(amount.negate())
                .balanceBefore(sourceBefore)
                .balanceAfter(sourceAfter)
                .entryType(LedgerEntryType.DEBIT)
                .build());

        ledgerDrafts.add(LedgerService.LedgerDraft.builder()
                .accountNumber(destination.getAccountNumber())
                .signedAmount(amount)
                .balanceBefore(destBefore)
                .balanceAfter(destAfter)
                .entryType(LedgerEntryType.CREDIT)
                .build());

        return creditTransaction;
    }

    /**
     * Deterministic lock order prevents deadlocks under concurrent A↔B transfers.
     */
    private AccountPair lockInStableOrder(String sourceNumber, String destNumber) {
        if (sourceNumber.compareTo(destNumber) < 0) {
            Account source = accountRepository.findByAccountNumberForUpdate(sourceNumber)
                    .orElseThrow(() -> new AccountNotFoundException(sourceNumber));
            Account dest = accountRepository.findByAccountNumberForUpdate(destNumber)
                    .orElseThrow(() -> new NotFoundException("error.destination.not_found"));
            return new AccountPair(source, dest);
        }
        Account dest = accountRepository.findByAccountNumberForUpdate(destNumber)
                .orElseThrow(() -> new NotFoundException("error.destination.not_found"));
        Account source = accountRepository.findByAccountNumberForUpdate(sourceNumber)
                .orElseThrow(() -> new AccountNotFoundException(sourceNumber));
        return new AccountPair(source, dest);
    }

    private record AccountPair(Account source, Account destination) {
    }
}
