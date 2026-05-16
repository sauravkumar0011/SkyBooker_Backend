package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SeatRequest {

    @NotNull
    private UUID flightId;

    @NotBlank
    private String seatNumber;

    @NotNull
    private SeatClass seatClass;

    @NotNull
    @Min(1)
    private Integer rowNumber;

    @NotBlank
    private String columnLetter;

    @NotNull
    private Boolean isWindow;

    @NotNull
    private Boolean isAisle;

    @NotNull
    private Boolean hasExtraLegroom;

    @NotNull
    @DecimalMin(value = "1.0", inclusive = false)
    private BigDecimal priceMultiplier;
}