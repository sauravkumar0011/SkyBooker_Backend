package com.skybooker.seat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmSeatRequest {
    @NotBlank
    private String holdReference;
}