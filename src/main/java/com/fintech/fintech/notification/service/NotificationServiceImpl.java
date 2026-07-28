package com.fintech.fintech.notification.service;

import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.enums.NotificationType;
import com.fintech.fintech.notification.delivery.EmailDeliveryService;
import com.fintech.fintech.notification.dto.NotificationDto;
import com.fintech.fintech.notification.entity.Notification;
import com.fintech.fintech.notification.repository.NotificationRepository;
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
