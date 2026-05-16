package com.skybooker.booking.client;

import com.skybooker.booking.config.FeignClientConfig;
import com.skybooker.booking.dto.external.PassengerSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "PASSENGER-SERVICE", configuration = FeignClientConfig.class)
public interface PassengerClient {

    @GetMapping("/passengers/booking/{bookingId}")
    List<PassengerSummaryResponse> getPassengersByBooking(@PathVariable("bookingId") UUID bookingId);
}
