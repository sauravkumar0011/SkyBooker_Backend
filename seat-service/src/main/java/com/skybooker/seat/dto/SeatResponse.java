package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SeatResponse {
    private UUID seatId;
    private UUID flightId;
    private String seatNumber;
    private SeatClass seatClass;
    private Integer rowNumber;
    private String columnLetter;
    private Boolean isWindow;
    private Boolean isAisle;
    private Boolean hasExtraLegroom;
    private SeatStatus status;
    private BigDecimal priceMultiplier;
    private LocalDateTime heldAt;
    private String holdReference;
}