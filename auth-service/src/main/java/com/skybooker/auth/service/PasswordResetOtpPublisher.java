package com.skybooker.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.skybooker.auth.config.RabbitMQConfig;
import com.skybooker.auth.dto.PasswordResetOtpEvent;

@Service
@RequiredArgsConstructor
public class PasswordResetOtpPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(PasswordResetOtpEvent event) {
        rabbitTemplate.convertAndSend(
        		  RabbitMQConfig.PASSWORD_RESET_OTP_QUEUE,
                  event
        );
    }
}