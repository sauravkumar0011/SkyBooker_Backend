package com.skybooker.notification.messaging;

import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.event.PaymentEvent;
import com.skybooker.notification.service.EmailService;
import com.skybooker.notification.service.NotificationService;
import com.skybooker.notification.service.TicketPdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private TicketPdfService ticketPdfService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Test
    void shouldSendProfessionalTicketEmailWithFrontendStyleAttachmentName() {
        PaymentEvent event = buildEvent();
        byte[] pdfBytes = "%PDF-sample".getBytes();
        when(ticketPdfService.generateTicketPdf(event)).thenReturn(pdfBytes);

        paymentEventListener.handlePaymentSuccess(event);

        ArgumentCaptor<NotificationRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getTitle()).isEqualTo("Payment Successful");
        assertThat(notificationCaptor.getValue().getRelatedBookingId()).isEqualTo(event.getBookingId());

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> attachmentCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendTicketEmail(
                eq(event.getRecipientEmail()),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                eq(pdfBytes),
                attachmentCaptor.capture()
        );

        assertThat(subjectCaptor.getValue()).contains("SkyBooker e-ticket").contains(event.getPnrCode());
        assertThat(bodyCaptor.getValue())
                .contains("Booking Confirmed")
                .contains("Passenger Details")
                .contains("Travel Notes")
                .contains(event.getFlightNumber())
                .contains(event.getPnrCode());
        assertThat(attachmentCaptor.getValue()).isEqualTo("SkyBooker-PNR123-Ticket.pdf");
    }

    private PaymentEvent buildEvent() {
        PaymentEvent event = new PaymentEvent();
        event.setBookingId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        event.setPaymentId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        event.setUserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        event.setAmount(new BigDecimal("6840.00"));
        event.setStatus("PAID");
        event.setTransactionId("TXN-884422");
        event.setRecipientEmail("traveller@example.com");
        event.setContactPhone("+91 9876543210");
        event.setPnrCode("PNR123");
        event.setPassengerCount(2);
        event.setFlightNumber("SB-204");
        event.setOriginAirportCode("DEL");
        event.setDestinationAirportCode("BOM");
        event.setDepartureTime("2026-05-20T09:15:00");
        event.setArrivalTime("2026-05-20T11:35:00");
        event.setBookedAt("2026-05-15T16:20:00");
        event.setPassengers(List.of(buildPassenger("Aarav Singh", "12A", "TKT1001"), buildPassenger("Ira Singh", "12B", "TKT1002")));
        return event;
    }

    private PaymentEvent.PassengerTicketInfo buildPassenger(String fullName, String seatNumber, String ticketNumber) {
        PaymentEvent.PassengerTicketInfo passenger = new PaymentEvent.PassengerTicketInfo();
        passenger.setFullName(fullName);
        passenger.setSeatNumber(seatNumber);
        passenger.setTicketNumber(ticketNumber);
        passenger.setNationality("Indian");
        passenger.setPassportNumber("N1234567");
        return passenger;
    }
}
