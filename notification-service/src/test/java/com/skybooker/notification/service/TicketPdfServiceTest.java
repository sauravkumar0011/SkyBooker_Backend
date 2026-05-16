package com.skybooker.notification.service;

import com.skybooker.notification.event.PaymentEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPdfServiceTest {

    private final TicketPdfService ticketPdfService = new TicketPdfService();

    @Test
    void shouldGenerateProfessionalTicketPdf() {
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(buildEvent());

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, StandardCharsets.ISO_8859_1)).startsWith("%PDF");
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
