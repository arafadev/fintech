package com.fintech.fintech.notification.repository;

import com.fintech.fintech.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
