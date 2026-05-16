package com.skybooker.notification.dto;

import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID notificationId;
    private UUID recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationChannel channel;
    private UUID relatedBookingId;
    private Boolean isRead;
    private LocalDateTime sentAt;
}