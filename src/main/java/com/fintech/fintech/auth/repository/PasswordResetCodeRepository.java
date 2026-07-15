package com.fintech.fintech.auth.repository;

import com.fintech.fintech.auth.entity.PasswordResetCode;
import com.fintech.fintech.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findByCode(String code);
    void deleteByUserId(Long userId);
}
