package com.fintech.fintech.config;

import com.fintech.fintech.account.service.AccountService;
import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.auth.repository.UserRepository;
import com.fintech.fintech.common.config.AppProperties;
import com.fintech.fintech.enums.AccountType;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = seedRoleIfMissing("ADMIN");
        seedRoleIfMissing("CUSTOMER");
        seedRoleIfMissing("AUDITOR");
        seedAdminIfMissing(adminRole);
    }

    private Role seedRoleIfMissing(String roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            log.info("Seeding role: {}", roleName);
            return roleRepository.save(Role.builder().name(roleName).build());
        });
    }

    private void seedAdminIfMissing(Role adminRole) {
        var admin = appProperties.getSeed().getAdmin();
        if (userRepository.findByEmail(admin.getEmail()).isPresent()) {
            log.info("Admin user already exists ({}), skip seeding", admin.getEmail());
            return;
        }

        log.info("Seeding the single system admin: {}", admin.getEmail());

        User savedAdmin = userRepository.save(User.builder()
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .email(admin.getEmail())
                .phoneNumber(admin.getPhone())
                .password(passwordEncoder.encode(admin.getPassword()))
                .roles(Set.of(adminRole))
                .active(true)
                .build());

        accountService.createAccount(AccountType.SAVINGS, savedAdmin);
        log.info("Admin seeded successfully. Login with email: {}", admin.getEmail());
    }
}
