package com.fintech.fintech.auth.service;

import com.fintech.fintech.account.entity.Account;
import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.dto.RegisterRequest;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.common.util.AfterCommit;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.enums.Currency;
import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.service.NotificationService;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final NotificationService notificationService;
    private final Messages messages;

    public Response<String> register(RegisterRequest request) {
        assertEmailAvailable(request.getEmail());
        assertPhoneAvailable(request.getPhoneNumber());

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new NotFoundException("error.role.customer_missing"));

        User savedUser = userRepository.save(User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(customerRole))
                .active(true)
                .build());

        Account savedAccount = accountService.createAccount(AccountType.SAVINGS, savedUser);
        scheduleWelcomeEmail(savedUser, savedAccount);

        return Response.ok(
                messages.get("success.register"),
                messages.get("success.register.data", savedAccount.getAccountNumber())
        );
    }

    private void assertEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("error.email.already_exists");
        }
    }

    private void assertPhoneAvailable(String phoneNumber) {
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new BadRequestException("error.phone.already_exists");
        }
    }

    private void scheduleWelcomeEmail(User user, Account account) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());
        vars.put("accountNumber", account.getAccountNumber());
        vars.put("accountType", AccountType.SAVINGS.name());
        vars.put("currency", Currency.USD.name());

        NotificationDto email = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Welcome to FinTech Bank")
                .templateName("account-created")
                .templateVariables(vars)
                .build();

        AfterCommit.run(() -> notificationService.sendEmail(email, user));
    }
}
