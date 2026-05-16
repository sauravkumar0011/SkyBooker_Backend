package com.skybooker.notification.repository;

import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientId(UUID recipientId);

    List<Notification> findByRecipientIdAndIsRead(UUID recipientId, Boolean isRead);

    long countByRecipientIdAndIsRead(UUID recipientId, Boolean isRead);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByRelatedBookingId(UUID relatedBookingId);
}