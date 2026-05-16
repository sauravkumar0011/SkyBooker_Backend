package com.skybooker.notification.service;

public interface EmailService {
	 void sendTicketEmail(String to, String subject, String htmlBody, byte[] pdfBytes, String attachmentFileName);
	 void sendEmail(String to, String subject, String body);
}
