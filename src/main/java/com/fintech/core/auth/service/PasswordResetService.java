package com.fintech.core.auth.service;

import com.fintech.core.auth.dto.ResetPasswordRequest;
import com.fintech.core.auth.entity.PasswordResetCode;
import com.fintech.core.auth.entity.User;
import com.fintech.core.auth.repository.PasswordResetCodeRepository;
import com.fintech.core.auth.repository.UserRepository;
import com.fintech.core.common.config.AppProperties;
import com.fintech.core.common.i18n.Messages;
import com.fintech.core.common.util.AfterCommit;
import com.fintech.core.exception.BadRequestException;
import com.fintech.core.exception.NotFoundException;
import com.fintech.core.notification.dto.NotificationDto;
import com.fintech.core.notification.service.NotificationService;
import com.fintech.core.response.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final CodeGenerator codeGenerator;
    private final ResetCodeHasher resetCodeHasher;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AppProperties appProperties;
    private final Messages messages;

    @Transactional
    public Response<?> requestReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("error.auth.user_not_found"));

        passwordResetCodeRepository.deleteByUserId(user.getId());

        String rawCode = codeGenerator.generateUniqueCode();
        passwordResetCodeRepository.save(PasswordResetCode.builder()
                .user(user)
                .codeHash(resetCodeHasher.hash(rawCode))
                .expiryDate(expiryFromNow())
                .used(false)
                .build());

        scheduleResetEmail(user, rawCode);
        return Response.ok(messages.get("success.password_reset_sent"));
    }

    @Transactional
    public Response<?> confirmReset(ResetPasswordRequest request) {
        String submittedHash = resetCodeHasher.hash(request.getCode());
        PasswordResetCode resetCode = passwordResetCodeRepository.findByCodeHash(submittedHash)
                .orElseThrow(() -> new BadRequestException("error.auth.reset_code_invalid"));

        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetCodeRepository.delete(resetCode);
            throw new BadRequestException("error.auth.reset_code_expired");
        }

        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(nextTokenVersion(user.getTokenVersion()));
        userRepository.save(user);
        passwordResetCodeRepository.delete(resetCode);

        schedulePasswordUpdatedEmail(user);
        return Response.ok(messages.get("success.password_updated"));
    }

    private LocalDateTime expiryFromNow() {
        int hours = appProperties.getPassword().getReset().getExpiryHours();
        return LocalDateTime.now().plusHours(hours);
    }

    private static long nextTokenVersion(Long current) {
        return current == null ? 1L : current + 1;
    }

    private void scheduleResetEmail(User user, String rawCode) {
        String resetLink = appProperties.getPassword().getReset().getLink();
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());
        vars.put("resetLink", resetLink + rawCode);

        NotificationDto email = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(vars)
                .build();

        AfterCommit.run(() -> notificationService.sendEmail(email, user));
    }

    private void schedulePasswordUpdatedEmail(User user) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getFirstName());

        NotificationDto email = NotificationDto.builder()
                .recipient(user.getEmail())
                .subject("Password Updated Successfully")
                .templateName("password-update-confirmation")
                .templateVariables(vars)
                .build();

        AfterCommit.run(() -> notificationService.sendEmail(email, user));
    }
}
