package com.skybooker.passenger.client;

import com.skybooker.passenger.config.FeignClientConfig;
import com.skybooker.passenger.dto.external.BookingSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "BOOKING-SERVICE", configuration = FeignClientConfig.class)
public interface BookingClient {

    @GetMapping("/bookings/{bookingId}")
    BookingSummaryResponse getBookingById(@PathVariable("bookingId") UUID bookingId);
}
