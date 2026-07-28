package com.fintech.fintech.auth.service.impl;

import com.fintech.fintech.auth.dto.LoginRequest;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.dto.ResetPasswordRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.auth.service.PasswordResetService;
import com.fintech.fintech.auth.service.RegistrationService;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock RegistrationService registrationService;
    @Mock PasswordResetService passwordResetService;
    @Mock Messages messages;

    @InjectMocks
    AuthServiceImpl authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name("CUSTOMER").build();
        lenient().when(messages.get(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_shouldDelegateToRegistrationService() {
        RegisterRequest request = RegisterRequest.builder().email("a@test.com").build();
        when(registrationService.register(request))
                .thenReturn(Response.ok("ok", "6611111111"));

        Response<String> response = authService.register(request);

        assertEquals(200, response.getStatusCode());
        verify(registrationService).register(request);
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void forgetPassword_shouldDelegateToPasswordResetService() {
        when(passwordResetService.requestReset("a@test.com"))
                .thenReturn(Response.ok("sent"));

        authService.forgetPassword("a@test.com");

        verify(passwordResetService).requestReset("a@test.com");
    }

    @Test
    void updatePasswordViaResetCode_shouldDelegateToPasswordResetService() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("code");
        request.setNewPassword("NewPass123");
        when(passwordResetService.confirmReset(request)).thenReturn(Response.ok("updated"));

        authService.updatePasswordViaResetCode(request);

        verify(passwordResetService).confirmReset(request);
    }

    @Test
    void login_shouldFail_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("any");

        assertThrows(NotFoundException.class, () -> authService.login(request));
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void login_shouldFail_whenPasswordWrong() {
        User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .password("encoded")
                .active(true)
                .roles(Set.of(customerRole))
                .build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("a@test.com");
        request.setPassword("wrong");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void login_shouldIssueToken_whenCredentialsValid() {
        User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .password("encoded")
                .active(true)
                .roles(Set.of(customerRole))
                .build();
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("a@test.com");
        request.setPassword("Password1");

        var response = authService.login(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("jwt-token", response.getData().getToken());
        assertTrue(response.getData().getRoles().contains("CUSTOMER"));
    }
}
