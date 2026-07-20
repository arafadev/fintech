package com.fintech.fintech.auth.service.impl;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.dto.LoginRequest;
import com.fintech.fintech.auth.dto.LoginResponse;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.dto.ResetPasswordRequest;
import com.fintech.fintech.auth.entity.PasswordResetCode;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.PasswordResetCodeRepository;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.auth.service.AuthService;
import com.fintech.fintech.auth.service.CodeGenerator;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import com.fintech.fintech.security.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fintech.fintech.enums.Currency;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final AccountService accountService;
    private final CodeGenerator codeGenerator;
    private final PasswordResetCodeRepository passwordResetCodeRepository;


    @Value("${password.reset.link}")
    private String resetLink;

    @Override
    public Response<String> register(RegisterRequest request) {

        // [فكرة 1]: تحديد وتجهيز الأدوار (Roles) بتاعة اليوزر الجديد في السيستم
        Set<Role> roles;

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new NotFoundException("CUSTOMER role not found"));
            roles = Collections.singleton(defaultRole);
        } else {
            roles = request.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new NotFoundException("Role Not Found: " + roleName)))
                    .collect(Collectors.toSet());
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email Already Present");
        }

        // [فكرة 3]: بناء كائن الـ User وتجهيز بياناته مع تشفير الباسورد فوراً قبل الحفظ
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // TODO AUTO GENERATE AN ACCOUNT NUMBER FOR THE USER
        Account savedAccount = accountService.createAccount(AccountType.SAVINGS, savedUser);

        Map<String, Object> accountVars = new HashMap<>();
        accountVars.put("name", savedUser.getFirstName());
        accountVars.put("accountNumber", savedAccount.getAccountNumber());
        accountVars.put("accountType", AccountType.SAVINGS.name());
        accountVars.put("currency", Currency.USD.name());

        NotificationDto accountCreatedEmail = NotificationDto.builder()
                .recipient(savedUser.getEmail())
                .subject("Welcome to FinTech Bank 🎉")
                .templateName("account-created")
                .templateVariables(accountVars)
                .build();

        notificationService.sendEmail(accountCreatedEmail, savedUser);

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your account has been created successfully")
                .data("Email of your account details has been sent to you. Your account number is: " + savedAccount.getAccountNumber())
                .build();
    }

    @Override
    public Response<LoginResponse> login(LoginRequest request) {

        String email = request.getEmail();
        String password = request.getPassword();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("Email Not found"));

    if(!passwordEncoder.matches(password, user.getPassword())){
        throw new BadRequestException("Password doesn't match");
    }

    String token = tokenService.generateToken(email);

    LoginResponse loginResponse = LoginResponse.builder()
            .roles(user.getRoles().stream().map(Role::getName).toList())
            .token(token)
            .build();

    return Response.<LoginResponse>builder()
            .statusCode(HttpStatus.OK.value())
            .message("login successful")
            .data(loginResponse)
            .build();

    }


    @Override
    @Transactional
    public Response<?> forgetPassword(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User Not Found"));
        passwordResetCodeRepository.deleteByUserId(user.getId());

        String code = codeGenerator.generateUniqueCode();

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash(code)
                .expiryDate(calculateExpiryDate())
                .used(false)
                .build();

        passwordResetCodeRepository.save(resetCode);

        //send email reset link out
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        templateVariables.put("resetLink", resetLink + code);


        NotificationDto notificationdto = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationdto, user);


        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password reset code sent to your email")
                .build();

    }

    @Override
    @Transactional
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {
        String code = resetPasswordRequest.getCode();
        String newPassword = resetPasswordRequest.getNewPassword();

        // Find and validate code

        PasswordResetCode resetCode = passwordResetCodeRepository.findByCodeHash(code)
                .orElseThrow(() -> new BadRequestException("Invalid reset code"));

        // Check expiration first
        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetCodeRepository.delete(resetCode); // Clean up expired code
            throw new BadRequestException("Reset code has expired");
        }


        //update the password
        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the code immediately after successful use
        passwordResetCodeRepository.delete(resetCode);


        // Send confirmation email
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDto confirmationEmail = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Password Updated Successfully")
                .templateName("password-update-confirmation")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(confirmationEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password updated successfully")
                .build();
    }


    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(5);
    }
}
