package com.fintech.core.transaction.service;

import com.fintech.core.account.entity.Account;
import com.fintech.core.account.repository.AccountRepository;
import com.fintech.core.auth.entity.User;
import com.fintech.core.auth.service.UserService;
import com.fintech.core.common.config.AppProperties;
import com.fintech.core.common.i18n.Messages;
import com.fintech.core.enums.AccountStatus;
import com.fintech.core.enums.TransactionType;
import com.fintech.core.exception.BadRequestException;
import com.fintech.core.exception.InsufficientBalanceException;
import com.fintech.core.exception.NotFoundException;
import com.fintech.core.ledger.service.LedgerService;
import com.fintech.core.response.Response;
import com.fintech.core.transaction.dto.TransactionDto;
import com.fintech.core.transaction.dto.TransactionRequest;
import com.fintech.core.transaction.entity.Transaction;
import com.fintech.core.transaction.operation.DepositOperation;
import com.fintech.core.transaction.operation.TransactionOperationRegistry;
import com.fintech.core.transaction.operation.TransferOperation;
import com.fintech.core.transaction.operation.WithdrawalOperation;
import com.fintech.core.transaction.repository.TransactionRepository;
import com.fintech.core.transaction.support.AccountTransactionGuard;
import com.fintech.core.transaction.support.TransactionLimitService;
import com.fintech.core.transaction.support.TransactionNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @Mock ModelMapper modelMapper;
    @Mock LedgerService ledgerService;
    @Mock TransactionNotificationService notificationService;
    @Mock Messages messages;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getLimits().setDailyOutgoing(new BigDecimal("50000.00"));
        appProperties.getLimits().setMaxSingleAmount(new BigDecimal("10000.00"));

        AccountTransactionGuard accountGuard = new AccountTransactionGuard(userService);
        TransactionLimitService limitService =
                new TransactionLimitService(transactionRepository, appProperties);

        TransactionOperationRegistry operationRegistry = new TransactionOperationRegistry(List.of(
                new DepositOperation(accountRepository, accountGuard),
                new WithdrawalOperation(accountRepository, accountGuard, limitService),
                new TransferOperation(accountRepository, accountGuard, limitService)
        ));

        lenient().when(messages.get(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));

        transactionService = new TransactionServiceImpl(
                transactionRepository,
                accountRepository,
                userService,
                modelMapper,
                ledgerService,
                operationRegistry,
                notificationService,
                messages
        );

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void transfer_shouldSucceed_whenBalanceEnough() {
        User owner = User.builder().id(1L).email("a@test.com").firstName("Ali").active(true).build();
        User receiver = User.builder().id(2L).email("b@test.com").firstName("Bob").active(true).build();

        Account source = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();
        Account dest = Account.builder()
                .accountNumber("6687654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(receiver)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-ok")).thenReturn(Optional.empty());
        when(userService.getCurrentLoggedInUser()).thenReturn(owner);
        when(accountRepository.findByAccountNumberForUpdate("6612345678")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("6687654321")).thenReturn(Optional.of(dest));
        when(transactionRepository.sumSuccessfulOutgoingSince(eq("6612345678"), any()))
                .thenReturn(BigDecimal.ZERO);
        AtomicLong idSeq = new AtomicLong(10L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(idSeq.getAndIncrement());
            }
            return t;
        });
        when(modelMapper.map(any(Transaction.class), eq(TransactionDto.class)))
                .thenReturn(new TransactionDto());

        Response<?> response = transactionService.createTransaction(
                transferRequest("6612345678", "6687654321", "100.00", "idem-ok"));

        assertEquals(200, response.getStatusCode());
        assertEquals(0, source.getBalance().compareTo(new BigDecimal("400.00")));
        assertEquals(0, dest.getBalance().compareTo(new BigDecimal("100.00")));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(ledgerService, times(2)).appendEntries(anyLong(), anyString(), anyLong(), anyList());
    }

    @Test
    void transfer_shouldFail_whenInsufficientBalance() {
        User owner = User.builder().id(1L).build();
        Account source = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("50.00"))
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();
        Account dest = Account.builder()
                .accountNumber("6687654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(User.builder().id(2L).build())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(userService.getCurrentLoggedInUser()).thenReturn(owner);
        when(accountRepository.findByAccountNumberForUpdate("6612345678")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("6687654321")).thenReturn(Optional.of(dest));
        when(transactionRepository.sumSuccessfulOutgoingSince(eq("6612345678"), any()))
                .thenReturn(BigDecimal.ZERO);

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createTransaction(
                        transferRequest("6612345678", "6687654321", "100.00", "idem-1")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldFail_whenSourceAccountNotFound() {
        when(transactionRepository.findByIdempotencyKey("idem-nf")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberForUpdate("6611111111")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> transactionService.createTransaction(
                        transferRequest("6611111111", "6687654321", "10.00", "idem-nf")));
    }

    @Test
    void transfer_shouldFail_whenSourceAccountClosed() {
        User owner = User.builder().id(1L).build();
        Account source = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.CLOSED)
                .user(owner)
                .build();
        Account dest = Account.builder()
                .accountNumber("6687654321")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(User.builder().id(2L).build())
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-closed")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberForUpdate("6612345678")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("6687654321")).thenReturn(Optional.of(dest));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transactionService.createTransaction(
                        transferRequest("6612345678", "6687654321", "10.00", "idem-closed")));
        assertTrue(ex.getMessage().toLowerCase().contains("closed"));
    }

    @Test
    void withdrawal_shouldFail_whenInsufficientBalance() {
        User owner = User.builder().id(1L).build();
        Account account = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("20.00"))
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-wd")).thenReturn(Optional.empty());
        when(userService.getCurrentLoggedInUser()).thenReturn(owner);
        when(accountRepository.findByAccountNumberForUpdate("6612345678")).thenReturn(Optional.of(account));
        when(transactionRepository.sumSuccessfulOutgoingSince(eq("6612345678"), any()))
                .thenReturn(BigDecimal.ZERO);

        TransactionRequest request = new TransactionRequest();
        request.setTransactionType(TransactionType.WITHDRAWAL);
        request.setAccountNumber("6612345678");
        request.setAmount(new BigDecimal("100.00"));
        request.setIdempotencyKey("idem-wd");

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createTransaction(request));
    }

    @Test
    void withdrawal_shouldFail_whenNotOwner() {
        User owner = User.builder().id(1L).build();
        User attacker = User.builder().id(99L).build();
        Account account = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-own")).thenReturn(Optional.empty());
        when(userService.getCurrentLoggedInUser()).thenReturn(attacker);
        when(accountRepository.findByAccountNumberForUpdate("6612345678")).thenReturn(Optional.of(account));

        TransactionRequest request = new TransactionRequest();
        request.setTransactionType(TransactionType.WITHDRAWAL);
        request.setAccountNumber("6612345678");
        request.setAmount(new BigDecimal("10.00"));
        request.setIdempotencyKey("idem-own");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transactionService.createTransaction(request));
        assertTrue(ex.getMessage().toLowerCase().contains("belong"));
    }

    @Test
    void createTransaction_shouldReturnExisting_whenIdempotencyKeyRepeated() {
        Transaction existing = Transaction.builder().id(5L).amount(new BigDecimal("10.00")).build();
        when(transactionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));
        when(modelMapper.map(existing, TransactionDto.class)).thenReturn(new TransactionDto());

        Response<?> response = transactionService.createTransaction(
                transferRequest("6612345678", "6687654321", "10.00", "idem-dup"));

        assertEquals("success.transaction_already_processed", response.getMessage());
        verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
    }

    private TransactionRequest transferRequest(String from, String to, String amount, String idem) {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionType(TransactionType.TRANSFER);
        request.setAccountNumber(from);
        request.setDestinationAccountNumber(to);
        request.setAmount(new BigDecimal(amount));
        request.setIdempotencyKey(idem);
        return request;
    }
}
