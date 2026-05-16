package com.skybooker.seat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReleaseSeatRequest {
    @NotBlank
    private String holdReference;
}