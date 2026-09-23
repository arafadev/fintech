package com.fintech.core.account.service.impl;

import com.fintech.core.account.entity.Account;
import com.fintech.core.account.repository.AccountRepository;
import com.fintech.core.account.support.AccountNumberGenerator;
import com.fintech.core.auth.entity.User;
import com.fintech.core.auth.service.UserService;
import com.fintech.core.common.i18n.Messages;
import com.fintech.core.enums.AccountStatus;
import com.fintech.core.exception.BadRequestException;
import com.fintech.core.exception.NotFoundException;
import com.fintech.core.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock UserService userService;
    @Mock ModelMapper modelMapper;
    @Mock AccountNumberGenerator accountNumberGenerator;
    @Mock Messages messages;

    @InjectMocks
    AccountServiceImpl accountService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(messages.get(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void closeAccount_shouldFail_whenBalanceGreaterThanZero() {
        Account account = Account.builder()
                .accountNumber("6612345678")
                .balance(new BigDecimal("10.00"))
                .status(AccountStatus.ACTIVE)
                .build();
        User user = User.builder().id(1L).accounts(new ArrayList<>(List.of(account))).build();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(accountRepository.findByAccountNumber("6612345678")).thenReturn(Optional.of(account));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> accountService.closeAccount("6612345678"));
        assertTrue(ex.getMessage().toLowerCase().contains("zero"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_shouldFail_whenNotOwner() {
        Account account = Account.builder()
                .accountNumber("6612345678")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        User user = User.builder().id(1L).accounts(new ArrayList<>()).build();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(accountRepository.findByAccountNumber("6612345678")).thenReturn(Optional.of(account));

        assertThrows(NotFoundException.class, () -> accountService.closeAccount("6612345678"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_shouldSucceed_whenOwnerAndZeroBalance() {
        Account account = Account.builder()
                .accountNumber("6612345678")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        User user = User.builder().id(1L).accounts(new ArrayList<>(List.of(account))).build();

        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(accountRepository.findByAccountNumber("6612345678")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Response<?> response = accountService.closeAccount("6612345678");

        assertEquals(200, response.getStatusCode());
        assertEquals(AccountStatus.CLOSED, account.getStatus());
        verify(accountRepository).save(account);
    }
}
