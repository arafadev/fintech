package com.fintech.core.auth.service;

import com.fintech.core.auth.repository.PasswordResetCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private final PasswordResetCodeRepository passwordResetCodeRepo;
    private final ResetCodeHasher resetCodeHasher;

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 32;


    public String generateUniqueCode() {
        String rawCode;
        String codeHash;
        do {
            rawCode = generateRandomCode();
            codeHash = resetCodeHasher.hash(rawCode);
        } while (passwordResetCodeRepo.findByCodeHash(codeHash).isPresent());

        return rawCode;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(ALPHA_NUMERIC.length());
            sb.append(ALPHA_NUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
