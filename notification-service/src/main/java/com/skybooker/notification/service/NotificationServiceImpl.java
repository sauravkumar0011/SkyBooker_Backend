package com.skybooker.notification.service;

import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.dto.NotificationResponse;
import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedBookingId(request.getRelatedBookingId())
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        
        return map(savedNotification);
    }

    @Override
    public List<NotificationResponse> getByRecipient(UUID recipientId) {
        return notificationRepository.findByRecipientId(recipientId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        return map(notificationRepository.save(notification));
    }

    @Override
    public void markAllRead(UUID recipientId) {
        List<Notification> notifications =
                notificationRepository.findByRecipientIdAndIsRead(recipientId, false);

        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    private NotificationResponse map(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel())
                .relatedBookingId(n.getRelatedBookingId())
                .isRead(n.getIsRead())
                .sentAt(n.getSentAt())
                .build();
    }
}