package com.skybooker.notification.service;

import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request);

    List<NotificationResponse> getByRecipient(UUID recipientId);

    long getUnreadCount(UUID recipientId);

    NotificationResponse markAsRead(UUID notificationId);

    void markAllRead(UUID recipientId);

    void deleteNotification(UUID notificationId);
}