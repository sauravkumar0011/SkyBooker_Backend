package com.skybooker.booking.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AddOnRequest {
    private String mealPreference;

    @Min(0)
    private Integer luggageKg;
}