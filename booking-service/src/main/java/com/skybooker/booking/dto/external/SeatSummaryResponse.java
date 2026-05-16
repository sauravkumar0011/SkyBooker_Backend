package com.skybooker.booking.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SeatSummaryResponse {
    private UUID seatId;
    private UUID flightId;
    private String seatNumber;
    private BigDecimal priceMultiplier;
    private String status;
    private String holdReference;
}
