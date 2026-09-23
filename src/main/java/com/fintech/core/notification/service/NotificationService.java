package com.fintech.core.notification.service;

import com.fintech.core.auth.entity.User;
import com.fintech.core.notification.dto.NotificationDto;

public interface NotificationService {
    void sendEmail(NotificationDto notificationDto, User user);
}
