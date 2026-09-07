package com.fintech.core.auth.service;

import com.fintech.core.auth.dto.ResetPasswordRequest;
import com.fintech.core.auth.entity.PasswordResetCode;
import com.fintech.core.auth.entity.User;
import com.fintech.core.auth.repository.PasswordResetCodeRepository;
import com.fintech.core.auth.repository.UserRepository;
import com.fintech.core.common.config.AppProperties;
import com.fintech.core.common.i18n.Messages;
import com.fintech.core.exception.BadRequestException;
import com.fintech.core.notification.service.NotificationService;
import com.fintech.core.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetCodeRepository passwordResetCodeRepository;
    @Mock CodeGenerator codeGenerator;
    @Mock ResetCodeHasher resetCodeHasher;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificationService notificationService;
    @Mock Messages messages;

    @Spy
    AppProperties appProperties = new AppProperties();

    @InjectMocks
    PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        appProperties.getPassword().getReset().setLink("http://localhost/reset?code=");
        appProperties.getPassword().getReset().setExpiryHours(5);
        lenient().when(messages.get(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
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
    void confirmReset_shouldFail_whenCodeInvalid() {
        when(resetCodeHasher.hash("bad-code")).thenReturn("hash-bad");
        when(passwordResetCodeRepository.findByCodeHash("hash-bad")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("bad-code");
        request.setNewPassword("NewPass123");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> passwordResetService.confirmReset(request));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void confirmReset_shouldFail_whenCodeExpired() {
        User user = User.builder().id(1L).email("a@test.com").firstName("Ali").tokenVersion(0L).build();
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash("hash-ok")
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(resetCodeHasher.hash("raw-code")).thenReturn("hash-ok");
        when(passwordResetCodeRepository.findByCodeHash("hash-ok")).thenReturn(Optional.of(resetCode));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("raw-code");
        request.setNewPassword("NewPass123");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> passwordResetService.confirmReset(request));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
        verify(passwordResetCodeRepository).delete(resetCode);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmReset_shouldSucceed_whenCodeValid() {
        User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .firstName("Ali")
                .password("old")
                .tokenVersion(0L)
                .build();
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash("hash-ok")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(resetCodeHasher.hash("raw-code")).thenReturn("hash-ok");
        when(passwordResetCodeRepository.findByCodeHash("hash-ok")).thenReturn(Optional.of(resetCode));
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-new");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("raw-code");
        request.setNewPassword("NewPass123");

        Response<?> response = passwordResetService.confirmReset(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("encoded-new", user.getPassword());
        assertEquals(1L, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(passwordResetCodeRepository).delete(resetCode);
        // email is scheduled after commit — invoke synchronizations to assert
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCommit());
        verify(notificationService).sendEmail(any(), eq(user));
    }
}
