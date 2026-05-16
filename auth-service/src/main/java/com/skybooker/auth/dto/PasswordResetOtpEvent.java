package com.skybooker.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetOtpEvent {
    private UUID userId;
    private String email;
    private String fullName;
    private String otp;
    private LocalDateTime expiresAt;
}