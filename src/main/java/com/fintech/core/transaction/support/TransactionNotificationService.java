package com.fintech.core.transaction.support;

import com.fintech.core.account.entity.Account;
import com.fintech.core.account.repository.AccountRepository;
import com.fintech.core.auth.entity.User;
import com.fintech.core.enums.TransactionType;
import com.fintech.core.exception.NotFoundException;
import com.fintech.core.notification.dto.NotificationDto;
import com.fintech.core.notification.service.NotificationService;
import com.fintech.core.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class TransactionNotificationService {

    private final NotificationService notificationService;
    private final AccountRepository accountRepository;

    public void notifyAfterTransaction(Transaction txn) {
        User user = txn.getAccount().getUser();

        if (txn.getTransactionType() == TransactionType.DEPOSIT) {
            send(user, "Credit Alert", "credit-alert", baseVars(user, txn, txn.getAccount()));
            return;
        }

        if (txn.getTransactionType() == TransactionType.WITHDRAWAL) {
            send(user, "Debit Alert", "debit-alert", baseVars(user, txn, txn.getAccount()));
            return;
        }

        if (txn.getTransactionType() == TransactionType.TRANSFER) {
            send(user, "Debit Alert", "debit-alert", baseVars(user, txn, txn.getAccount()));

            Account destination = accountRepository.findByAccountNumber(txn.getDestinationAccount())
                    .orElseThrow(() -> new NotFoundException("error.destination.not_found"));
            User receiver = destination.getUser();
            send(receiver, "Credit Alert", "credit-alert", baseVars(receiver, txn, destination));
        }
    }

    private Map<String, Object> baseVars(User user, Transaction txn, Account account) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());
        vars.put("amount", txn.getAmount());
        vars.put("accountNumber", account.getAccountNumber());
        vars.put("date", txn.getTransactionDate());
        vars.put("balance", account.getBalance());
        return vars;
    }

    private void send(User user, String subject, String template, Map<String, Object> vars) {
        notificationService.sendEmail(NotificationDto.builder()
                .recipient(user.getEmail())
                .subject(subject)
                .templateName(template)
                .templateVariables(vars)
                .build(), user);
    }
}
