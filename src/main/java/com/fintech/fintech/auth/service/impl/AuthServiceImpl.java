package com.fintech.fintech.auth.service.impl;

import com.fintech.fintech.auth.dto.LoginRequest;
import com.fintech.fintech.auth.dto.LoginResponse;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.dto.ResetPasswordRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.auth.service.AuthService;
import com.fintech.fintech.auth.service.PasswordResetService;
import com.fintech.fintech.auth.service.RegistrationService;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final Messages messages;

    @Override
    public Response<String> register(RegisterRequest request) {
        return registrationService.register(request);
    }

    @Override
    public Response<LoginResponse> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("error.auth.email_not_found"));

        if (!user.isActive()) {
            throw new BadRequestException("error.auth.account_disabled");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("error.auth.password_mismatch");
        }

        LoginResponse payload = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(tokenService.generateToken(user))
                .build();

        return Response.ok(messages.get("success.login"), payload);
    }

    @Override
    public Response<?> forgetPassword(String email) {
        return passwordResetService.requestReset(email);
    }

    @Override
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest request) {
        return passwordResetService.confirmReset(request);
    }
}
