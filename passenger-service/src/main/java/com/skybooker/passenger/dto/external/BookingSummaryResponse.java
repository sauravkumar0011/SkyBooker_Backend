package com.skybooker.passenger.dto.external;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingSummaryResponse {
    private UUID bookingId;
    private UUID flightId;
    private List<UUID> seatIds;
    private Integer totalPassengers;
    private String status;
}
