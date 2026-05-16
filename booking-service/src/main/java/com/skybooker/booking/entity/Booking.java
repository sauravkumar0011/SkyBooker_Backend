package com.skybooker.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "pnr_Code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID flightId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id", nullable = false)
    private List<UUID> seatIds = new ArrayList<>();

    @Column(name = "seat_id")
    private UUID legacySeatId;

    @Column(nullable = false, unique = true, length = 6)
    private String pnrCode;

    @Column(nullable = false)
    private String holdReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxes;

    @Column(nullable = false)
    private Integer totalPassengers;

    @Column
    private String mealPreference;

    @Column
    private Integer luggageKg;

    @Column(nullable = false)
    private String contactEmail;

    @Column(nullable = false)
    private String contactPhone;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Column
    private UUID paymentId;

    @PrePersist
    @PreUpdate
    public void syncDerivedFields() {
        if (this.bookedAt == null) {
            this.bookedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
        if (this.seatIds == null) {
            this.seatIds = new ArrayList<>();
        }
        if (this.totalPassengers == null) {
            this.totalPassengers = this.seatIds.size();
        }
        this.legacySeatId = (this.seatIds == null || this.seatIds.isEmpty())
                ? null
                : this.seatIds.get(0);
    }
}
