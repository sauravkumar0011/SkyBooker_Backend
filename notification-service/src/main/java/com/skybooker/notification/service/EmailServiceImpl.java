package com.skybooker.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;

	@Override
	public void sendTicketEmail(String to, String subject, String htmlBody, byte[] pdfBytes, String attachmentFileName) {
		try {
			MimeMessage message = mailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);

			helper.addAttachment(attachmentFileName, new ByteArrayResource(pdfBytes));

			mailSender.send(message);
		} catch (Exception e) {
			throw new RuntimeException("Email sending failed: " + e.getMessage());
		}
	}

	@Override
	public void sendEmail(String to, String subject, String htmlBody) {
		try {
			MimeMessage message = mailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(message, false);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);

			mailSender.send(message);

		} catch (Exception e) {
			throw new RuntimeException("Email sending failed: " + e.getMessage());
		}
	}
}
