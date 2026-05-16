package com.skybooker.notification.controller;

import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.dto.NotificationResponse;
import com.skybooker.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return new ResponseEntity<>(notificationService.send(request), HttpStatus.CREATED);
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(@PathVariable UUID recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<Long> getUnreadCount(@PathVariable UUID recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<String> markAllRead(@PathVariable UUID recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<String> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok("Notification deleted successfully");
    }
}