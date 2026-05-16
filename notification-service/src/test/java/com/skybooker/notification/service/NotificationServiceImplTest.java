package com.skybooker.notification.service;

import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.dto.NotificationResponse;
import com.skybooker.notification.entity.*;
import com.skybooker.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationServiceImpl service;

    private UUID notificationId;
    private UUID userId;

    private Notification notification;

    @BeforeEach
    void setup() {
        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        notification = Notification.builder()
                .notificationId(notificationId)
                .recipientId(userId)
                .type(NotificationType.PAYMENT_SUCCESS)
                .title("Test")
                .message("Test message")
                .channel(NotificationChannel.APP)
                .isRead(false)
                .build();
    }

    @Test
    void send_success() {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(userId);
        req.setType(NotificationType.PAYMENT_SUCCESS);
        req.setTitle("Test");
        req.setMessage("Test message");
        req.setChannel(NotificationChannel.APP);

        when(repository.save(any())).thenReturn(notification);

        NotificationResponse res = service.send(req);

        assertNotNull(res);
        assertEquals(userId, res.getRecipientId());
        verify(repository).save(any());
    }

    @Test
    void getByRecipient_success() {
        when(repository.findByRecipientId(userId)).thenReturn(List.of(notification));

        List<NotificationResponse> list = service.getByRecipient(userId);

        assertEquals(1, list.size());
    }

    @Test
    void getUnreadCount_success() {
        when(repository.countByRecipientIdAndIsRead(userId, false)).thenReturn(5L);

        long count = service.getUnreadCount(userId);

        assertEquals(5, count);
    }

    @Test
    void markAsRead_success() {
        when(repository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationResponse res = service.markAsRead(notificationId);

        assertTrue(res.getIsRead());
    }

    @Test
    void markAsRead_notFound() {
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.markAsRead(notificationId));
    }

    @Test
    void markAllRead_success() {
        List<Notification> list = new ArrayList<>();
        list.add(notification);

        when(repository.findByRecipientIdAndIsRead(userId, false)).thenReturn(list);

        service.markAllRead(userId);

        assertTrue(list.get(0).getIsRead());
        verify(repository).saveAll(list);
    }

    @Test
    void deleteNotification_success() {
        service.deleteNotification(notificationId);

        verify(repository).deleteById(notificationId);
    }
}