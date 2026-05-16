package com.skybooker.passenger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "passengers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"booking_id", "seat_id"}),
        @UniqueConstraint(columnNames = "ticket_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID passengerId;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID seatId;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String gender;

    private String passportNumber;

    private String nationality;

    @Column(nullable = false, unique = true)
    private String ticketNumber;
}
