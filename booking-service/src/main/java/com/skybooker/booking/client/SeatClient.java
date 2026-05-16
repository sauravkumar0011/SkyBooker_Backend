package com.skybooker.booking.client;

import com.skybooker.booking.config.FeignClientConfig;
import com.skybooker.booking.dto.external.BulkSeatOperationRequest;
import com.skybooker.booking.dto.external.SeatLookupRequest;
import com.skybooker.booking.dto.external.SeatSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "SEAT-SERVICE", configuration = FeignClientConfig.class)
public interface SeatClient {

    @PostMapping("/seats/details/bulk")
    List<SeatSummaryResponse> getSeatsByIds(@RequestBody SeatLookupRequest request);

    @PostMapping("/seats/hold/bulk")
    List<SeatSummaryResponse> holdSeats(@RequestBody BulkSeatOperationRequest request);

    @PostMapping("/seats/confirm/bulk")
    List<SeatSummaryResponse> confirmSeats(@RequestBody BulkSeatOperationRequest request);

    @PostMapping("/seats/release/bulk")
    List<SeatSummaryResponse> releaseSeats(@RequestBody BulkSeatOperationRequest request);
}
