package com.skybooker.booking.client;

import com.skybooker.booking.dto.external.FlightSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.skybooker.booking.config.FeignClientConfig;

import java.util.UUID;

@FeignClient(name = "FLIGHT-SERVICE", configuration = FeignClientConfig.class)
public interface FlightClient {

    @GetMapping("/flights/{flightId}")
    FlightSummaryResponse getFlightById(@PathVariable("flightId") UUID flightId);

    @PutMapping("/flights/{flightId}/decrement-seats")
    Object decrementSeats(@PathVariable("flightId") UUID flightId, @RequestParam("count") int count);

    @PutMapping("/flights/{flightId}/increment-seats")
    Object incrementSeats(@PathVariable("flightId") UUID flightId, @RequestParam("count") int count);
}
