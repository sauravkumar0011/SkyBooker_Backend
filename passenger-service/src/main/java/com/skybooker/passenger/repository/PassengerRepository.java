package com.skybooker.passenger.repository;

import com.skybooker.passenger.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PassengerRepository extends JpaRepository<Passenger, UUID> {

    List<Passenger> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    boolean existsByBookingIdAndSeatId(UUID bookingId, UUID seatId);

}
