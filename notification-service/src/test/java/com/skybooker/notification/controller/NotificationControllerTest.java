package com.skybooker.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.notification.dto.*;
import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationType;
import com.skybooker.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService service;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setup() {
        NotificationController controller = new NotificationController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    private NotificationRequest validRequest() {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(userId);
        req.setType(NotificationType.PAYMENT_SUCCESS);
        req.setTitle("Test");
        req.setMessage("Test message");
        req.setChannel(NotificationChannel.APP);
        return req;
    }

    @Test
    void send_success() throws Exception {
        when(service.send(any())).thenReturn(NotificationResponse.builder().build());

        mockMvc.perform(post("/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void getByRecipient_success() throws Exception {
        when(service.getByRecipient(userId))
                .thenReturn(List.of(NotificationResponse.builder().build()));

        mockMvc.perform(get("/notifications/recipient/{id}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_success() throws Exception {
        when(service.getUnreadCount(userId)).thenReturn(3L);

        mockMvc.perform(get("/notifications/recipient/{id}/unread-count", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void markAsRead_success() throws Exception {
        when(service.markAsRead(notificationId))
                .thenReturn(NotificationResponse.builder().isRead(true).build());

        mockMvc.perform(put("/notifications/{id}/read", notificationId))
                .andExpect(status().isOk());
    }

    @Test
    void markAllRead_success() throws Exception {
        mockMvc.perform(put("/notifications/recipient/{id}/read-all", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read"));
    }

    @Test
    void deleteNotification_success() throws Exception {
        mockMvc.perform(delete("/notifications/{id}", notificationId))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification deleted successfully"));
    }
}