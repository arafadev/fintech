package com.fintech.core.notification.service;

import com.fintech.core.auth.entity.User;
import com.fintech.core.enums.NotificationType;
import com.fintech.core.notification.delivery.EmailDeliveryService;
import com.fintech.core.notification.dto.NotificationDto;
import com.fintech.core.notification.entity.Notification;
import com.fintech.core.notification.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final EmailDeliveryService emailDeliveryService;

    @Override
    @Async
    public void sendEmail(NotificationDto notificationDTO, User user) {
        try {
            emailDeliveryService.deliver(notificationDTO);
            archive(notificationDTO, user);
        } catch (MessagingException e) {
            log.error("Error while sending email: {}", e.getMessage());
        }
    }

    private void archive(NotificationDto dto, User user) {
        notificationRepo.save(Notification.builder()
                .recipient(dto.getRecipient())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .type(NotificationType.EMAIL)
                .user(user)
                .build());
    }
}
