package com.skybooker.notification.messaging;

import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import com.skybooker.notification.config.RabbitMQConfig;
import com.skybooker.notification.event.PasswordResetOtpEvent;
import com.skybooker.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordResetOtpListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.PASSWORD_RESET_OTP_QUEUE)
    public void handlePasswordResetOtp(PasswordResetOtpEvent event) {

        String subject = "SkyBooker Password Reset OTP";

        String body = """
                <h2>SkyBooker Password Reset</h2>

                <p>Hello <b>%s</b>,</p>

                <p>We received a request to reset your SkyBooker account password.</p>

                <p>Your OTP is:</p>

                <h1 style="letter-spacing: 4px;">%s</h1>

                <p>This OTP is valid for <b>5 minutes</b>.</p>

                <p>If you did not request this, please ignore this email.</p>

                <br>

                <p>Regards,<br>SkyBooker Team</p>
                """.formatted(event.getFullName(), event.getOtp());

        emailService.sendEmail(event.getEmail(), subject, body);
    }
}