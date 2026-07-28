package com.fintech.fintech.transaction.service;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.common.util.AfterCommit;
import com.fintech.fintech.enums.TransactionStatus;
import com.fintech.fintech.exception.AccountNotFoundException;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.ledger.enums.LedgerEntryType;
import com.fintech.fintech.ledger.service.LedgerService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionDto;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.entity.Transaction;
import com.fintech.fintech.transaction.operation.TransactionOperationRegistry;
import com.fintech.fintech.transaction.repository.TransactionRepository;
import com.fintech.fintech.transaction.support.Money;
import com.fintech.fintech.transaction.support.TransactionNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final LedgerService ledgerService;
    private final TransactionOperationRegistry operationRegistry;
    private final TransactionNotificationService notificationService;
    private final Messages messages;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Response<?> createTransaction(@Valid TransactionRequest request) {
        assertIdempotencyKeyPresent(request);

        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return alreadyProcessed(existing.get());
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(Money.of(request.getAmount()));
        transaction.setDescription(request.getDescription());
        transaction.setIdempotencyKey(request.getIdempotencyKey());

        List<LedgerService.LedgerDraft> ledgerDrafts = new ArrayList<>();
        Transaction transferCreditLeg = operationRegistry
                .require(request.getTransactionType())
                .execute(request, transaction, ledgerDrafts);

        transaction.setStatus(TransactionStatus.SUCCESS);
        if (transferCreditLeg != null) {
            transferCreditLeg.setStatus(TransactionStatus.SUCCESS);
        }

        Transaction savedTxn;
        Transaction savedCreditLeg = null;
        try {
            savedTxn = transactionRepository.save(transaction);
            if (transferCreditLeg != null) {
                savedCreditLeg = transactionRepository.save(transferCreditLeg);
            }
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::alreadyProcessed)
                    .orElseThrow(() -> ex);
        }

        appendLedger(request.getIdempotencyKey(), savedTxn, savedCreditLeg, ledgerDrafts);

        // Touch lazy associations before the TX closes (needed by after-commit email)
        savedTxn.getAccount().getUser().getEmail();
        savedTxn.getAccount().getBalance();

        AfterCommit.run(() -> notificationService.notifyAfterTransaction(savedTxn));

        return Response.ok(
                messages.get("success.transaction"),
                modelMapper.map(savedTxn, TransactionDto.class)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<TransactionDto>> getTransactionsForMyAccount(String accountNumber, int page, int size) {
        User user = userService.getCurrentLoggedInUser();
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("error.account.not_belong");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepository.findByAccount_AccountNumber(accountNumber, pageable);

        List<TransactionDto> dtos = txns.getContent().stream()
                .map(txn -> modelMapper.map(txn, TransactionDto.class))
                .toList();

        Map<String, Serializable> meta = Map.of(
                "currentPage", txns.getNumber(),
                "totalItems", txns.getTotalElements(),
                "totalPages", txns.getTotalPages(),
                "pageSize", txns.getSize()
        );

        return Response.ok(messages.get("success.transactions_retrieved"), dtos, meta);
    }

    private void assertIdempotencyKeyPresent(TransactionRequest request) {
        if (!StringUtils.hasText(request.getIdempotencyKey())) {
            throw new BadRequestException("error.idempotency.required");
        }
        if (request.getIdempotencyKey().length() > 64) {
            throw new BadRequestException("error.idempotency.too_long");
        }
    }

    private Response<?> alreadyProcessed(Transaction txn) {
        return Response.ok(
                messages.get("success.transaction_already_processed"),
                modelMapper.map(txn, TransactionDto.class)
        );
    }

    private void appendLedger(
            String correlationId,
            Transaction debitOrSingle,
            Transaction creditLeg,
            List<LedgerService.LedgerDraft> drafts
    ) {
        User actor = userService.getCurrentLoggedInUser();

        if (creditLeg == null) {
            ledgerService.appendEntries(debitOrSingle.getId(), correlationId, actor.getId(), drafts);
            return;
        }

        List<LedgerService.LedgerDraft> debitDrafts = drafts.stream()
                .filter(d -> d.entryType() == LedgerEntryType.DEBIT)
                .toList();
        List<LedgerService.LedgerDraft> creditDrafts = drafts.stream()
                .filter(d -> d.entryType() == LedgerEntryType.CREDIT)
                .toList();

        ledgerService.appendEntries(debitOrSingle.getId(), correlationId, actor.getId(), debitDrafts);
        ledgerService.appendEntries(creditLeg.getId(), correlationId, actor.getId(), creditDrafts);
    }
}
