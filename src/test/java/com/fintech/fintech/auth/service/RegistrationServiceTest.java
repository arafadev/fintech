package com.fintech.fintech.auth.service;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AccountService accountService;
    @Mock NotificationService notificationService;
    @Mock Messages messages;

    @InjectMocks
    RegistrationService registrationService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name("CUSTOMER").build();
        lenient().when(messages.get(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void register_shouldAlwaysAssignCustomerRole_neverAdminFromRequest() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(accountService.createAccount(eq(AccountType.SAVINGS), any(User.class)))
                .thenReturn(Account.builder().accountNumber("6611111111").build());

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ali")
                .lastName("Hassan")
                .email("new@test.com")
                .phoneNumber("01012345678")
                .password("Password1")
                .build();

        Response<String> response = registrationService.register(request);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        Set<Role> roles = userCaptor.getValue().getRoles();
        assertEquals(1, roles.size());
        assertTrue(roles.stream().allMatch(r -> "CUSTOMER".equals(r.getName())));
        verify(roleRepository, never()).findByName("ADMIN");
        verify(roleRepository).findByName("CUSTOMER");
    }

    @Test
    void register_shouldFail_whenPhoneAlreadyExists() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("01012345678"))
                .thenReturn(Optional.of(User.builder().id(1L).phoneNumber("01012345678").build()));

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ali")
                .lastName("Hassan")
                .email("new@test.com")
                .phoneNumber("01012345678")
                .password("Password1")
                .build();

        assertThrows(BadRequestException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFail_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("new@test.com"))
                .thenReturn(Optional.of(User.builder().id(1L).email("new@test.com").build()));

        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.com")
                .phoneNumber("01012345678")
                .password("Password1")
                .build();

        assertThrows(BadRequestException.class, () -> registrationService.register(request));
        verify(userRepository, never()).findByPhoneNumber(anyString());
        verify(userRepository, never()).save(any());
    }
}
