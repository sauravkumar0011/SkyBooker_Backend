package com.skybooker.passenger.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skybooker.passenger.client.BookingClient;
import com.skybooker.passenger.dto.PassengerDetailsRequest;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.PassengerUpdateRequest;
import com.skybooker.passenger.dto.external.BookingSummaryResponse;
import com.skybooker.passenger.entity.Passenger;
import com.skybooker.passenger.exception.InvalidPassengerOperationException;
import com.skybooker.passenger.exception.ResourceNotFoundException;
import com.skybooker.passenger.repository.PassengerRepository;

import lombok.*;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final BookingClient bookingClient;

    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    @Transactional
    public List<PassengerResponse> addPassengers(PassengerRequest request) {
        BookingSummaryResponse booking = bookingClient.getBookingById(request.getBookingId());

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new InvalidPassengerOperationException("Passenger details can only be added for pending bookings");
        }

        if (passengerRepository.existsByBookingId(request.getBookingId())) {
            throw new InvalidPassengerOperationException("Passengers already exist for this booking");
        }

        List<PassengerDetailsRequest> passengers = request.getPassengers();
        if (booking.getSeatIds() == null || booking.getSeatIds().isEmpty()) {
            throw new InvalidPassengerOperationException("Booking does not contain any seats");
        }

        if (passengers.size() != booking.getSeatIds().size()) {
            throw new InvalidPassengerOperationException("Passenger count must exactly match the number of booked seats");
        }

        Set<UUID> validSeatIds = new HashSet<>(booking.getSeatIds());
        Set<UUID> seenSeatIds = new HashSet<>();
        List<Passenger> entities = new ArrayList<>();

        for (PassengerDetailsRequest passengerRequest : passengers) {
            UUID seatId = passengerRequest.getSeatId();
            if (!validSeatIds.contains(seatId)) {
                throw new InvalidPassengerOperationException("Passenger seat does not belong to the booking");
            }

            if (!seenSeatIds.add(seatId)) {
                throw new InvalidPassengerOperationException("Duplicate passenger seat mapping is not allowed");
            }

            if (passengerRepository.existsByBookingIdAndSeatId(request.getBookingId(), seatId)) {
                throw new InvalidPassengerOperationException("A passenger is already mapped to seat " + seatId);
            }

            entities.add(Passenger.builder()
                    .bookingId(request.getBookingId())
                    .seatId(seatId)
                    .firstName(passengerRequest.getFirstName().trim())
                    .lastName(passengerRequest.getLastName().trim())
                    .dateOfBirth(passengerRequest.getDateOfBirth())
                    .gender(passengerRequest.getGender())
                    .passportNumber(passengerRequest.getPassportNumber())
                    .nationality(passengerRequest.getNationality().trim())
                    .ticketNumber(generateTicketNumber())
                    .build());
        }

        return passengerRepository.saveAll(entities).stream().map(this::map).toList();
    }

    @Override
    public List<PassengerResponse> getByBooking(UUID bookingId) {
        return passengerRepository.findByBookingId(bookingId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public PassengerResponse getById(UUID passengerId) {
        Passenger p = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        return map(p);
    }

    @Override
    public PassengerResponse updatePassenger(UUID passengerId, PassengerUpdateRequest request) {
        Passenger p = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));

        p.setFirstName(request.getFirstName().trim());
        p.setLastName(request.getLastName().trim());
        p.setDateOfBirth(request.getDateOfBirth());
        p.setGender(request.getGender());
        p.setPassportNumber(request.getPassportNumber().trim());
        p.setNationality(request.getNationality().trim());

        return map(passengerRepository.save(p));
    }

    @Override
    public void deletePassenger(UUID passengerId) {
        if (!passengerRepository.existsById(passengerId)) {
            throw new ResourceNotFoundException("Passenger not found");
        }
        passengerRepository.deleteById(passengerId);
    }

    private PassengerResponse map(Passenger p) {
        return PassengerResponse.builder()
                .passengerId(p.getPassengerId())
                .bookingId(p.getBookingId())
                .seatId(p.getSeatId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .passportNumber(p.getPassportNumber())
                .nationality(p.getNationality())
                .ticketNumber(p.getTicketNumber())
                .build();
    }
}
