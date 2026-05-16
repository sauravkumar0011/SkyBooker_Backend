package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SeatCountResponse {
    private UUID flightId;
    private SeatClass seatClass;
    private long availableCount;
}