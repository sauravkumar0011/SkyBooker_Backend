package com.skybooker.seat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"flight_Id", "seat_Number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID seatId;

    @Column(nullable = false)
    private UUID flightId;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    @Column(name = "seat_row",nullable = false)
    private Integer rowNumber;

    @Column(nullable = false)
    private String columnLetter;

    @Column(nullable = false)
    private Boolean isWindow;

    @Column(nullable = false)
    private Boolean isAisle;

    @Column(nullable = false)
    private Boolean hasExtraLegroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal priceMultiplier;
    
    private LocalDateTime heldAt;

    private String holdReference;
}