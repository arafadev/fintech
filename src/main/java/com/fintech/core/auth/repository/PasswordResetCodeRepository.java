package com.fintech.core.auth.repository;

import com.fintech.core.auth.entity.PasswordResetCode;
import com.fintech.core.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findByCodeHash(String code);
    void deleteByUserId(Long userId);
}
