package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BulkSeatRequest {

    @NotNull
    private UUID flightId;

    @NotNull
    private SeatClass seatClass;

    @NotNull
    @Min(1)
    private Integer fromRow;

    @NotNull
    @Min(1)
    private Integer toRow;

    @NotBlank
    private String seatColumns; // example: ABCDEF

    @NotNull
    @DecimalMin(value = "1.0", inclusive = false)
    private BigDecimal priceMultiplier;

    @NotNull
    private Boolean extraLegroom;
}