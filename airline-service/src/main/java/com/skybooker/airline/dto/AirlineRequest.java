package com.skybooker.airline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 2, max = 3)
    private String iataCode;

    @Size(max = 4)
    private String icaoCode;

    private String country;

    @NotBlank
    @Email
    private String contactEmail;

    private String contactPhone;
}