package com.skybooker.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FareSummaryResponse {
    private List<UUID> seatIds;
    private BigDecimal baseFare;
    private BigDecimal taxes;
    private BigDecimal baggageCharge;
    private BigDecimal mealCharge;
    private BigDecimal totalFare;
    private Integer totalPassengers;
}
