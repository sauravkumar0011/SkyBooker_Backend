package com.skybooker.passenger.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;


@Data
@Builder
public class PassengerResponse {

    private UUID passengerId;
    private UUID bookingId;
    private UUID seatId;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String passportNumber;
    private String nationality;

    private String ticketNumber;
}