package com.fintech.fintech.notification.service;

import com.fintech.fintech.auth.entity.User;
import com.fintech.fintech.notification.dto.NotificationDto;

public interface NotificationService {
    void sendEmail(NotificationDto notificationDto, User user);
}
