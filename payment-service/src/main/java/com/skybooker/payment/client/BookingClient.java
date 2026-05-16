package com.skybooker.payment.client;

import com.skybooker.payment.config.FeignClientConfig;
import com.skybooker.payment.dto.external.BookingTicketDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;


@FeignClient(name = "booking-service", configuration = FeignClientConfig.class)
public interface BookingClient {

    @GetMapping("/bookings/{bookingId}/ticket-details")
    BookingTicketDetailsResponse getBookingTicketDetails(@PathVariable UUID bookingId);

    @PutMapping("/bookings/{bookingId}/confirm")
    Object confirmBooking(@PathVariable UUID bookingId);

    @PutMapping("/bookings/{bookingId}/cancel")
    Object cancelBooking(@PathVariable UUID bookingId);
}
