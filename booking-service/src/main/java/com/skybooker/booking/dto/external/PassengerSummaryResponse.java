package com.skybooker.booking.dto.external;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PassengerSummaryResponse {
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
