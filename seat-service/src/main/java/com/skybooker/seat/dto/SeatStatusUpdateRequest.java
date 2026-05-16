package com.skybooker.seat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SeatStatusUpdateRequest {

	@NotNull
	private UUID seatId;
}
