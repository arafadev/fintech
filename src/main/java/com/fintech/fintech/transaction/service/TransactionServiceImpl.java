package com.fintech.fintech.transaction.service;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.repository.AccountRepository;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.service.UserService;
import com.fintech.fintech.enums.TransactionStatus;
import com.fintech.fintech.enums.TransactionType;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.InsufficientBalanceException;
import com.fintech.fintech.exception.InvalidTransactionException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.transaction.dto.TransactionDto;
import com.fintech.fintech.transaction.dto.TransactionRequest;
import com.fintech.fintech.transaction.entity.Transaction;
import com.fintech.fintech.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest request) {

        Transaction transaction = new Transaction();

        transaction.setTransactionType(request.getTransactionType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        switch (request.getTransactionType()) {
            case DEPOSIT -> handleDeposit(request, transaction);
            case WITHDRAWAL -> handleWithdrawal(request, transaction);
            case TRANSFER -> handleTransfer(request, transaction);
            default -> throw new InvalidTransactionException("Invalid transaction type");
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction savedTxn = transactionRepository.save(transaction);

        //send notification out
        sendTransactionNotifications(savedTxn);


        return Response.builder()
                .statusCode(200)
                .message("Transaction successful")
                .build();
    }



    private void handleDeposit(TransactionRequest request, Transaction transaction) {

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        transaction.setAccount(account);
        accountRepository.save(account);
    }

    private void handleWithdrawal(TransactionRequest request, Transaction transaction) {

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        transaction.setAccount(account);
        accountRepository.save(account);
    }

    private void handleTransfer(TransactionRequest request, Transaction transaction) {

        Account sourceAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Account destination = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new NotFoundException("Destination Account not found"));

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account");

        }

        // Deduct from source
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);


        // Add to destination
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepository.save(destination);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(sourceAccount.getAccountNumber());
        transaction.setDestinationAccount(destination.getAccountNumber());
    }
    private void sendTransactionNotifications(Transaction tnx) {

        User user = tnx.getAccount().getUser();
        String subject;
        String template;

        Map<String, Object> templateVariables = new HashMap<>();

        templateVariables.put("name", user.getFirstName());
        templateVariables.put("amount", tnx.getAmount());
        templateVariables.put("accountNumber", tnx.getAccount().getAccountNumber());
        templateVariables.put("date", tnx.getTransactionDate());
        templateVariables.put("balance", tnx.getAccount().getBalance());

        if (tnx.getTransactionType() == TransactionType.DEPOSIT) {
            subject = "Credit Alert";
            template = "credit-alert";

            NotificationDto notificationEmailToSendOut = NotificationDto.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();
            notificationService.sendEmail(notificationEmailToSendOut, user);

        } else if (tnx.getTransactionType() == TransactionType.WITHDRAWAL) {

            subject = "Debit Alert";
            template = "debit-alert";

            NotificationDto notificationEmailToSendOut = NotificationDto.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOut, user);
        } else if (tnx.getTransactionType() == TransactionType.TRANSFER) {

            subject = "Debit Alert";
            template = "debit-alert";


            NotificationDto notificationEmailToSendOut = NotificationDto.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOut, user);


            // Receiver CREDIT alert
            Account destination = accountRepository.findByAccountNumber(tnx.getDestinationAccount())
                    .orElseThrow(() -> new NotFoundException("Destination account not found"));


            User receiver = destination.getUser();


            Map<String, Object> recvVars = new HashMap<>();
            recvVars.put("name", receiver.getFirstName());
            recvVars.put("amount", tnx.getAmount());
            recvVars.put("accountNumber", destination.getAccountNumber());
            recvVars.put("date", tnx.getTransactionDate());
            recvVars.put("balance", destination.getBalance());


            NotificationDto notificationEmailToSendOutToReveiver = NotificationDto.builder()
                    .recipient(receiver.getEmail())
                    .subject("Credit Alert")
                    .templateName("credit-alert")
                    .templateVariables(recvVars)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOutToReveiver, user);
        }
    }
    @Override
    @Transactional
    public Response<List<TransactionDto>> getTransactionsForMyAccount(String accountNumber, int page, int size) {

        // Get the currently logged-in user
        User user = userService.getCurrentLoggedInUser();

        // Find the account by its number
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        //make sure he account belongs to the user. an extra security check
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Account does not belong to the authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepository.findByAccount_AccountNumber(accountNumber, pageable);

        List<TransactionDto> transactionDTOS = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDto.class))
                .toList();

        return Response.<List<TransactionDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Transactions retrieved")
                .data(transactionDTOS)
                .meta(Map.of(
                        "currentPage", txns.getNumber(),
                        "totalItems", txns.getTotalElements(),
                        "totalPages", txns.getTotalPages(),
                        "pageSize", txns.getSize()
                ))
                .build();
    }


}
