package com.skybooker.booking.repository;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    Optional<Booking> findByPnrCode(String pnrCode);

    List<Booking> findByFlightId(UUID flightId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    long countByFlightIdAndStatus(UUID flightId, BookingStatus status);

    boolean existsByPnrCode(String pnrCode);

    boolean existsByHoldReference(String holdReference);
    
}