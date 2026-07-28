package com.fintech.fintech.auth.service.impl;

import com.fintech.fintech.auth.dto.UpdatePasswordRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.auth.storage.ProfilePictureStorage;
import com.fintech.fintech.auth.support.CurrentUserResolver;
import com.fintech.fintech.auth.support.ProfilePictureValidator;
import com.fintech.fintech.aws.S3Service;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ModelMapper modelMapper;
    @Mock TokenService tokenService;
    @Mock ProfilePictureStorage profilePictureStorage;
    @Mock ProfilePictureValidator profilePictureValidator;
    @Mock CurrentUserResolver currentUserResolver;
    @Mock Messages messages;
    @Mock S3Service s3Service;

    @InjectMocks
    UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("encoded-old")
                .firstName("Ali")
                .tokenVersion(0L)
                .active(true)
                .build();
        when(currentUserResolver.auth()).thenReturn(user);
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
    void updatePassword_shouldFail_whenOldPasswordMismatch() {
        when(passwordEncoder.matches("wrong-old", "encoded-old")).thenReturn(false);

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("wrong-old");
        request.setNewPassword("NewPass123");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.updatePassword(request));
        assertTrue(ex.getMessage().contains("old_password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_shouldSucceed_whenOldPasswordMatches() {
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-new");

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("NewPass123");

        var response = userService.updatePassword(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("encoded-new", user.getPassword());
        assertEquals(1L, user.getTokenVersion());
        verify(userRepository).save(user);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCommit());
        verify(notificationService).sendEmail(any(), eq(user));
    }
}
