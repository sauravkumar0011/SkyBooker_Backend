package com.skybooker.notification.messaging;

import com.skybooker.notification.config.RabbitMQConfig;
import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationType;
import com.skybooker.notification.event.PaymentEvent;
import com.skybooker.notification.service.EmailService;
import com.skybooker.notification.service.NotificationService;
import com.skybooker.notification.service.TicketPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final DateTimeFormatter EMAIL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter EMAIL_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' HH:mm", Locale.ENGLISH);

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TicketPdfService ticketPdfService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(event.getUserId());
        request.setType(NotificationType.PAYMENT_SUCCESS);
        request.setTitle("Payment Successful");
        request.setMessage("Your group booking payment of INR " + event.getAmount()
                + " was successful. PNR: " + event.getPnrCode()
                + ". Transaction ID: " + event.getTransactionId());
        request.setChannel(NotificationChannel.APP);
        request.setRelatedBookingId(event.getBookingId());

        notificationService.send(request);

        try {
            byte[] ticketPdf = ticketPdfService.generateTicketPdf(event);
            emailService.sendTicketEmail(
                    event.getRecipientEmail(),
                    buildSubject(event),
                    buildTicketEmailBody(event),
                    ticketPdf,
                    buildAttachmentFileName(event)
            );
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUND_QUEUE)
    public void handlePaymentRefund(PaymentEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(event.getUserId());
        request.setType(NotificationType.PAYMENT_REFUND);
        request.setTitle("Refund Processed");
        request.setMessage("Your refund of INR " + event.getAmount()
                + " has been processed.");
        request.setChannel(NotificationChannel.APP);
        request.setRelatedBookingId(event.getBookingId());

        notificationService.send(request);
    }

    private String buildSubject(PaymentEvent event) {
        return "Your SkyBooker e-ticket is confirmed | PNR " + display(event.getPnrCode(), "Pending");
    }

    private String buildAttachmentFileName(PaymentEvent event) {
        String pnr = display(event.getPnrCode(), "Ticket").replaceAll("[^A-Za-z0-9_-]", "");
        return "SkyBooker-" + (pnr.isBlank() ? "Ticket" : pnr) + "-Ticket.pdf";
    }

    private String buildTicketEmailBody(PaymentEvent event) {
        List<PaymentEvent.PassengerTicketInfo> passengers = safePassengers(event);
        String passengerRows = passengers.stream()
                .map(passenger -> """
                        <tr>
                            <td style="padding:10px 12px;border-bottom:1px solid #e7eef5;color:#26415d;">%s</td>
                            <td style="padding:10px 12px;border-bottom:1px solid #e7eef5;color:#26415d;">%s</td>
                            <td style="padding:10px 12px;border-bottom:1px solid #e7eef5;color:#26415d;">%s</td>
                            <td style="padding:10px 12px;border-bottom:1px solid #e7eef5;color:#26415d;">%s</td>
                        </tr>
                        """.formatted(
                        escape(display(passenger.getFullName(), "Passenger")),
                        escape(display(passenger.getSeatNumber(), "--")),
                        escape(display(passenger.getTicketNumber(), "--")),
                        escape(display(passenger.getNationality(), "Not provided"))
                ))
                .collect(Collectors.joining());

        if (passengerRows.isBlank()) {
            passengerRows = """
                    <tr>
                        <td style="padding:12px;border-bottom:1px solid #e7eef5;color:#26415d;" colspan="4">
                            Passenger details will be available with your booking record.
                        </td>
                    </tr>
                    """;
        }

        String routeCode = display(event.getOriginAirportCode(), "ORG") + "-" + display(event.getDestinationAirportCode(), "DST");
        String journeyDate = formatDate(event.getDepartureTime());
        String departureTime = formatTime(event.getDepartureTime());
        String arrivalTime = formatTime(event.getArrivalTime());
        String bookedAt = formatDateTime(event.getBookedAt());
        String duration = formatDuration(event.getDepartureTime(), event.getArrivalTime());

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#eef4fa;font-family:Arial,Helvetica,sans-serif;color:#18324f;">
                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background:#eef4fa;width:100%%;">
                        <tr>
                            <td align="center" style="padding:24px 12px;">
                                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="720" style="width:720px;max-width:100%%;background:#ffffff;border:1px solid #d8e6f1;border-radius:20px;">
                                    <tr>
                                        <td style="padding:28px 32px 20px;border-bottom:1px solid #e4eef6;">
                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%">
                                                <tr>
                                                    <td align="left">
                                                        <div style="font-size:30px;font-weight:800;color:#1777d3;line-height:1;">SkyBooker</div>
                                                        <div style="margin-top:6px;font-size:13px;color:#5c738d;">Booking Confirmation and E-Ticket</div>
                                                    </td>
                                                    <td align="right">
                                                        <div style="display:inline-block;padding:10px 16px;background:#edf9ee;border:1px solid #bfe3c4;border-radius:999px;color:#2f8f40;font-size:18px;font-weight:700;">
                                                            Booking Confirmed
                                                        </div>
                                                        <div style="margin-top:10px;font-size:13px;color:#6c839d;">
                                                            <div>Booking Date: %s</div>
                                                            <div>Booking Time: %s</div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:24px 32px 8px;">
                                            <p style="margin:0 0 10px;font-size:15px;line-height:1.7;">
                                                Dear Traveller, your payment has been received successfully and your itinerary for
                                                <span style="color:#1674d3;font-weight:700;">%s</span> is now confirmed.
                                            </p>
                                            <div style="font-size:14px;color:#304961;font-weight:700;">Flight %s</div>
                                            <div style="margin-top:4px;font-size:13px;color:#7388a0;">Booking ID %s</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:8px 32px 0;">
                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background:#f8fbff;border:1px solid #dbe8f4;border-radius:18px;">
                                                <tr>
                                                    <td width="32%%" style="padding:20px 22px;vertical-align:top;">
                                                        <div style="font-size:28px;font-weight:800;color:#1280d8;">%s</div>
                                                        <div style="margin-top:8px;font-size:20px;font-weight:700;color:#18324f;">%s</div>
                                                        <div style="margin-top:6px;font-size:12px;color:#607891;">%s</div>
                                                    </td>
                                                    <td width="36%%" style="padding:20px 10px;vertical-align:middle;text-align:center;">
                                                        <div style="font-size:12px;color:#607891;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">Journey</div>
                                                        <div style="margin-top:6px;font-size:16px;font-weight:700;color:#304961;">%s</div>
                                                        <div style="margin-top:12px;border-top:1px solid #bdd5e9;"></div>
                                                        <div style="margin-top:10px;font-size:14px;color:#5c738d;">Non-stop</div>
                                                    </td>
                                                    <td width="32%%" style="padding:20px 22px;vertical-align:top;text-align:right;">
                                                        <div style="font-size:28px;font-weight:800;color:#1280d8;">%s</div>
                                                        <div style="margin-top:8px;font-size:20px;font-weight:700;color:#18324f;">%s</div>
                                                        <div style="margin-top:6px;font-size:12px;color:#607891;">%s</div>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:24px 32px 0;">
                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%">
                                                <tr>
                                                    <td width="50%%" style="padding:0 10px 0 0;vertical-align:top;">
                                                        <div style="padding:16px 18px;background:#ffffff;border:1px solid #d8e9f4;">
                                                            <div style="font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#6c839d;">Booking Summary</div>
                                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin-top:10px;font-size:14px;color:#304961;">
                                                                <tr><td style="padding:6px 0;color:#6c839d;">PNR</td><td align="right" style="padding:6px 0;font-weight:700;">%s</td></tr>
                                                                <tr><td style="padding:6px 0;color:#6c839d;">Transaction ID</td><td align="right" style="padding:6px 0;font-weight:700;">%s</td></tr>
                                                                <tr><td style="padding:6px 0;color:#6c839d;">Amount Paid</td><td align="right" style="padding:6px 0;font-weight:700;">%s</td></tr>
                                                                <tr><td style="padding:6px 0;color:#6c839d;">Passengers</td><td align="right" style="padding:6px 0;font-weight:700;">%s</td></tr>
                                                                <tr><td style="padding:6px 0;color:#6c839d;">Booked On</td><td align="right" style="padding:6px 0;font-weight:700;">%s</td></tr>
                                                            </table>
                                                        </div>
                                                    </td>
                                                    
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:24px 32px 0;">
                                            <div style="padding:10px 14px;background:#dff2ff;border:1px solid #c4e1f4;color:#1777d3;font-size:20px;font-weight:700;">
                                                Passenger Details
                                            </div>
                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="border-collapse:collapse;border:1px solid #d8e9f4;border-top:none;">
                                                <thead>
                                                    <tr style="background:#edf7ff;color:#294d73;font-size:13px;font-weight:700;">
                                                        <th align="left" style="padding:10px 12px;border-bottom:1px solid #d8e9f4;">Passenger</th>
                                                        <th align="left" style="padding:10px 12px;border-bottom:1px solid #d8e9f4;">Seat</th>
                                                        <th align="left" style="padding:10px 12px;border-bottom:1px solid #d8e9f4;">Ticket Number</th>
                                                        <th align="left" style="padding:10px 12px;border-bottom:1px solid #d8e9f4;">Nationality</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    %s
                                                </tbody>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:24px 32px 32px;">
                                            <div style="padding:16px 18px;background:#f8fbff;border-top:1px solid #d8e9f6;font-size:13px;line-height:1.7;color:#5d748e;">
                                                <div style="font-weight:700;color:#304961;margin-bottom:4px;">Need-to-know before travel</div>
                                                Check-in opens as per airline policy. Cabin baggage and checked baggage allowances remain subject to your airline fare rules.
                                                Please keep your ticket, ID proof, and essential medicines accessible while travelling.
                                                If you need any assistance, reply to this email with your Booking ID <b>%s</b>.
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escape(formatDate(event.getBookedAt())),
                escape(formatTime(event.getBookedAt())),
                escape(routeCode),
                escape(display(event.getFlightNumber(), "Pending")),
                escape(String.valueOf(event.getBookingId())),
                escape(display(event.getOriginAirportCode(), "ORG")),
                escape(departureTime),
                escape(journeyDate),
                escape(duration),
                escape(display(event.getDestinationAirportCode(), "DST")),
                escape(arrivalTime),
                escape(journeyDate),
                escape(display(event.getPnrCode(), "--")),
                escape(display(event.getTransactionId(), "--")),
                escape(formatCurrency(event.getAmount())),
                escape(formatPassengerCount(event, passengers)),
                escape(bookedAt),
                passengerRows,
                escape(String.valueOf(event.getBookingId()))
        );
    }

    private List<PaymentEvent.PassengerTicketInfo> safePassengers(PaymentEvent event) {
        return event.getPassengers() == null ? Collections.emptyList() : event.getPassengers();
    }

    private String formatPassengerCount(PaymentEvent event, List<PaymentEvent.PassengerTicketInfo> passengers) {
        int count = event.getPassengerCount() != null
                ? event.getPassengerCount()
                : (!passengers.isEmpty() ? passengers.size() : 0);
        return count + " Adult" + (count == 1 ? "" : "s");
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "INR " + formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String formatDate(String value) {
        ZonedDateTime dateTime = parseDateTime(value);
        return dateTime == null ? display(value, "--") : EMAIL_DATE_FORMATTER.format(dateTime);
    }

    private String formatTime(String value) {
        ZonedDateTime dateTime = parseDateTime(value);
        return dateTime == null ? display(value, "--") : EMAIL_TIME_FORMATTER.format(dateTime);
    }

    private String formatDateTime(String value) {
        ZonedDateTime dateTime = parseDateTime(value);
        return dateTime == null ? display(value, "--") : EMAIL_DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatDuration(String departureTime, String arrivalTime) {
        ZonedDateTime departure = parseDateTime(departureTime);
        ZonedDateTime arrival = parseDateTime(arrivalTime);
        if (departure == null || arrival == null) {
            return "Duration to be updated";
        }

        long totalMinutes = Math.max(0, Duration.between(departure, arrival).toMinutes());
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format(Locale.ENGLISH, "%02dh %02dm", hours, minutes);
    }

    private ZonedDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toZonedDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value).atZone(ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
