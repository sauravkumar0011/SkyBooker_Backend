package com.skybooker.notification.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.skybooker.notification.event.PaymentEvent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

@Service
public class TicketPdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' HH:mm", Locale.ENGLISH);

    public byte[] generateTicketPdf(PaymentEvent event) {
        try {
            List<PaymentEvent.PassengerTicketInfo> passengers = safePassengers(event);
            String passengerRows = passengers.stream()
                    .map(passenger -> """
                            <tr>
                                <td>%s</td>
                                <td>%s</td>
                                <td>%s</td>
                                <td>%s</td>
                                <td>%s</td>
                            </tr>
                            """.formatted(
                            escape(display(passenger.getFullName(), "Passenger")),
                            escape(display(passenger.getSeatNumber(), "--")),
                            escape(display(passenger.getTicketNumber(), "--")),
                            escape(display(passenger.getNationality(), "Not provided")),
                            escape(display(passenger.getPassportNumber(), "Not provided"))
                    ))
                    .collect(Collectors.joining());

            if (passengerRows.isBlank()) {
                passengerRows = """
                        <tr>
                            <td colspan="5" class="empty-row">Passenger details are not available for this booking.</td>
                        </tr>
                        """;
            }

            String routeCode = display(event.getOriginAirportCode(), "ORG") + "-" + display(event.getDestinationAirportCode(), "DST");
            String bookedDate = formatDate(event.getBookedAt());
            String bookedTime = formatTime(event.getBookedAt());
            String journeyDate = formatDate(event.getDepartureTime());
            String departureTime = formatTime(event.getDepartureTime());
            String arrivalTime = formatTime(event.getArrivalTime());
            String duration = formatDuration(event.getDepartureTime(), event.getArrivalTime());

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8" />
                        <style>
                            @page {
                                size: A4;
                                margin: 20px;
                            }
                            body {
                                margin: 0;
                                padding: 0;
                                background: #eef4fa;
                                color: #18324f;
                                font-family: Arial, Helvetica, sans-serif;
                                font-size: 12px;
                            }
                            .ticket-shell {
                                background: #ffffff;
                                border: 1px solid #d4e6f3;
                                border-radius: 20px;
                                padding: 24px;
                            }
                            .topbar {
                                width: 100%%;
                                border-bottom: 1px solid #d8e9f6;
                                padding-bottom: 18px;
                            }
                            .brand-name {
                                font-size: 28px;
                                font-weight: 800;
                                color: #1777d3;
                                line-height: 1;
                            }
                            .brand-subtitle {
                                margin-top: 4px;
                                font-size: 12px;
                                color: #5c738d;
                            }
                            .status-wrap {
                                text-align: right;
                            }
                            .status-badge {
                                display: inline-block;
                                padding: 9px 14px;
                                border: 1px solid #bde4c0;
                                border-radius: 999px;
                                background: #edf9ee;
                                color: #2f8f40;
                                font-size: 18px;
                                font-weight: 700;
                            }
                            .status-meta {
                                margin-top: 10px;
                                color: #6c839d;
                                font-size: 12px;
                                line-height: 1.6;
                            }
                            .greeting {
                                padding: 18px 0 8px;
                                line-height: 1.7;
                                font-size: 13px;
                            }
                            .route-accent {
                                color: #1674d3;
                                font-weight: 700;
                            }
                            .journey-heading {
                                margin-top: 8px;
                            }
                            .journey-flight {
                                font-size: 14px;
                                font-weight: 700;
                                color: #304961;
                            }
                            .journey-booking {
                                margin-top: 4px;
                                font-size: 12px;
                                color: #7388a0;
                            }
                            .journey-card {
                                width: 100%%;
                                margin-top: 14px;
                                border: 1px solid #dbe8f4;
                                background: #f8fbff;
                                border-radius: 18px;
                            }
                            .journey-card td {
                                padding: 20px 22px;
                                vertical-align: top;
                            }
                            .journey-card .airport {
                                font-size: 28px;
                                font-weight: 800;
                                color: #1280d8;
                            }
                            .journey-card .clock {
                                margin-top: 8px;
                                font-size: 20px;
                                font-weight: 700;
                                color: #18324f;
                            }
                            .journey-card .date {
                                margin-top: 6px;
                                font-size: 11px;
                                color: #607891;
                            }
                            .journey-card .middle {
                                text-align: center;
                                vertical-align: middle;
                            }
                            .journey-label {
                                font-size: 11px;
                                font-weight: 700;
                                letter-spacing: 1px;
                                text-transform: uppercase;
                                color: #607891;
                            }
                            .journey-duration {
                                margin-top: 8px;
                                font-size: 15px;
                                font-weight: 700;
                                color: #304961;
                            }
                            .journey-rule {
                                margin: 12px 0 10px;
                                border-top: 1px solid #bdd5e9;
                            }
                            .journey-type {
                                font-size: 13px;
                                color: #5c738d;
                            }
                            .section-heading {
                                margin-top: 22px;
                                padding: 10px 14px;
                                background: #dff2ff;
                                border: 1px solid #c4e1f4;
                                color: #1777d3;
                                font-size: 18px;
                                font-weight: 700;
                            }
                            .summary-grid {
                                width: 100%%;
                                margin-top: 12px;
                            }
                            .summary-card {
                                width: 48.5%%;
                                padding: 0 0 0 0;
                                vertical-align: top;
                            }
                            .summary-card-inner {
                                border: 1px solid #d8e9f4;
                                background: #ffffff;
                                padding: 16px 18px;
                            }
                            .mini-title {
                                font-size: 11px;
                                font-weight: 700;
                                letter-spacing: 1px;
                                text-transform: uppercase;
                                color: #6c839d;
                            }
                            .meta-table {
                                width: 100%%;
                                margin-top: 10px;
                                border-collapse: collapse;
                            }
                            .meta-table td {
                                padding: 6px 0;
                                font-size: 13px;
                            }
                            .meta-label {
                                color: #6c839d;
                            }
                            .meta-value {
                                text-align: right;
                                color: #304961;
                                font-weight: 700;
                            }
                            .notes-list {
                                margin: 12px 0 0 18px;
                                padding: 0;
                                color: #304961;
                                line-height: 1.7;
                                font-size: 13px;
                            }
                            .passenger-table {
                                width: 100%%;
                                border-collapse: collapse;
                                border: 1px solid #d8e9f4;
                                border-top: none;
                            }
                            .passenger-table th,
                            .passenger-table td {
                                padding: 10px 12px;
                                border: 1px solid #d8e9f4;
                                text-align: left;
                                vertical-align: top;
                                font-size: 12px;
                                color: #26415d;
                            }
                            .passenger-table th {
                                background: #edf7ff;
                                color: #294d73;
                                font-weight: 700;
                            }
                            .empty-row {
                                text-align: center;
                                color: #607891;
                            }
                            .fare-box {
                                margin-top: 12px;
                                border: 1px solid #d8e9f4;
                                background: #ffffff;
                                padding: 16px 18px;
                            }
                            .fare-row {
                                width: 100%%;
                                border-collapse: collapse;
                            }
                            .fare-row td {
                                padding: 8px 0;
                                font-size: 13px;
                                color: #304961;
                            }
                            .fare-total td {
                                padding-top: 14px;
                                border-top: 1px dashed #d5e4ef;
                                font-size: 16px;
                                font-weight: 700;
                                color: #1676d1;
                            }
                            .footer-note {
                                margin-top: 24px;
                                padding-top: 16px;
                                border-top: 1px solid #d8e9f6;
                                font-size: 12px;
                                line-height: 1.7;
                                color: #5d748e;
                            }
                            .footer-note strong {
                                color: #304961;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="ticket-shell">
                            <table class="topbar" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td>
                                        <div class="brand-name">SkyBooker</div>
                                        <div class="brand-subtitle">E-Ticket Itinerary</div>
                                    </td>
                                    <td class="status-wrap">
                                        <div class="status-badge">Booking Confirmed</div>
                                        <div class="status-meta">
                                            <div>Booking Date: %s</div>
                                            <div>Booking Time: %s</div>
                                        </div>
                                    </td>
                                </tr>
                            </table>

                            <div class="greeting">
                                Dear Passenger, your flight booking for <span class="route-accent">%s</span> is confirmed.
                                <div class="journey-heading">
                                    <div class="journey-flight">Flight %s</div>
                                    <div class="journey-booking">Booking ID %s</div>
                                </div>
                            </div>

                            <table class="journey-card" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td width="32%%">
                                        <div class="airport">%s</div>
                                        <div class="clock">%s</div>
                                        <div class="date">%s</div>
                                    </td>
                                    <td width="36%%" class="middle">
                                        <div class="journey-label">Journey</div>
                                        <div class="journey-duration">%s</div>
                                        <div class="journey-rule"></div>
                                        <div class="journey-type">Non-stop</div>
                                    </td>
                                    <td width="32%%" style="text-align:right;">
                                        <div class="airport">%s</div>
                                        <div class="clock">%s</div>
                                        <div class="date">%s</div>
                                    </td>
                                </tr>
                            </table>

                            <div class="section-heading">Booking Summary</div>
                            <table class="summary-grid" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td class="summary-card">
                                        <div class="summary-card-inner">
                                            <div class="mini-title">Itinerary Details</div>
                                            <table class="meta-table" cellpadding="0" cellspacing="0" border="0">
                                                <tr><td class="meta-label">PNR</td><td class="meta-value">%s</td></tr>
                                                <tr><td class="meta-label">Payment ID</td><td class="meta-value">%s</td></tr>
                                                <tr><td class="meta-label">Transaction ID</td><td class="meta-value">%s</td></tr>
                                                <tr><td class="meta-label">Passengers</td><td class="meta-value">%s</td></tr>
                                                <tr><td class="meta-label">Contact Phone</td><td class="meta-value">%s</td></tr>
                                                <tr><td class="meta-label">Booked At</td><td class="meta-value">%s</td></tr>
                                            </table>
                                        </div>
                                    </td>
                                    <td width="3%%"></td>
                                    <td class="summary-card">
                                        <div class="summary-card-inner">
                                            <div class="mini-title">Travel Guidance</div>
                                            <ul class="notes-list">
                                                <li>Please report at the airport at least 2 hours before departure.</li>
                                                <li>Carry a valid government-issued photo ID for each passenger.</li>
                                                <li>Keep this ticket PDF accessible during check-in and boarding.</li>
                                                <li>Baggage allowance is subject to your selected fare rules and airline policy.</li>
                                            </ul>
                                        </div>
                                    </td>
                                </tr>
                            </table>

                            <div class="section-heading">Passengers - %s</div>
                            <table class="passenger-table" cellpadding="0" cellspacing="0" border="0">
                                <thead>
                                    <tr>
                                        <th>Passenger</th>
                                        <th>Seat No</th>
                                        <th>Ticket Number</th>
                                        <th>Nationality</th>
                                        <th>Passport</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    %s
                                </tbody>
                            </table>

                            <div class="section-heading">Fare Details</div>
                            <div class="fare-box">
                                <table class="fare-row" cellpadding="0" cellspacing="0" border="0">
                                    <tr>
                                        <td>Total Amount Paid</td>
                                        <td style="text-align:right;font-weight:700;">%s</td>
                                    </tr>
                                    <tr>
                                        <td>Payment Status</td>
                                        <td style="text-align:right;">%s</td>
                                    </tr>
                                    <tr class="fare-total">
                                        <td>Final Confirmation</td>
                                        <td style="text-align:right;">Ticket Issued</td>
                                    </tr>
                                </table>
                            </div>

                            <div class="footer-note">
                                <strong>Important:</strong> Please verify the passenger names, ticket numbers, and travel timings before departure.
                                Cabin baggage and checked baggage allowances can vary by airline and fare class.
                                Keep passports, IDs, medicines, valuables, and essential travel documents in an easily accessible cabin bag.
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(
                    escape(bookedDate),
                    escape(bookedTime),
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
                    escape(display(String.valueOf(event.getPaymentId()), "--")),
                    escape(display(event.getTransactionId(), "--")),
                    escape(formatPassengerCount(event, passengers)),
                    escape(display(event.getContactPhone(), "--")),
                    escape(formatDateTime(event.getBookedAt())),
                    escape(formatPassengerCount(event, passengers)),
                    passengerRows,
                    escape(formatCurrency(event.getAmount())),
                    escape(display(event.getStatus(), "Confirmed"))
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ticket PDF generation failed: " + e.getMessage(), e);
        }
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
        return dateTime == null ? display(value, "--") : DATE_FORMATTER.format(dateTime);
    }

    private String formatTime(String value) {
        ZonedDateTime dateTime = parseDateTime(value);
        return dateTime == null ? display(value, "--") : TIME_FORMATTER.format(dateTime);
    }

    private String formatDateTime(String value) {
        ZonedDateTime dateTime = parseDateTime(value);
        return dateTime == null ? display(value, "--") : DATE_TIME_FORMATTER.format(dateTime);
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
