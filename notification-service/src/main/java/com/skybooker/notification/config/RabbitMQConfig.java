package com.skybooker.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_REFUND_QUEUE = "payment.refund.queue";
    public static final String PASSWORD_RESET_OTP_QUEUE = "password.reset.otp.queue";
    
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue paymentRefundQueue() {
        return new Queue(PAYMENT_REFUND_QUEUE, true);
    }
    
    @Bean
    public Queue passwordResetOtpQueue() {
        return new Queue(PASSWORD_RESET_OTP_QUEUE, true);
    }
}