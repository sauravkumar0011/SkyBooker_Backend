package com.skybooker.notification.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetOtpEvent {
    private UUID userId;
    private String email;
    private String fullName;
    private String otp;
    private LocalDateTime expiresAt;
}