package com.skybooker.booking.service;

import com.skybooker.booking.client.FlightClient;
import com.skybooker.booking.client.PassengerClient;
import com.skybooker.booking.client.SeatClient;
import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.dto.BookingStatusUpdateRequest;
import com.skybooker.booking.dto.BookingTicketDetailsResponse;
import com.skybooker.booking.dto.FareSummaryResponse;
import com.skybooker.booking.dto.external.BulkSeatOperationRequest;
import com.skybooker.booking.dto.external.FlightSummaryResponse;
import com.skybooker.booking.dto.external.PassengerSummaryResponse;
import com.skybooker.booking.dto.external.SeatLookupRequest;
import com.skybooker.booking.dto.external.SeatSummaryResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.exception.InvalidBookingOperationException;
import com.skybooker.booking.exception.ResourceNotFoundException;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.18);
    private static final BigDecimal BAGGAGE_RATE_PER_KG = BigDecimal.valueOf(600);
    private static final BigDecimal MEAL_RATE_PER_PASSENGER = BigDecimal.valueOf(0);

    private final BookingRepository bookingRepository;
    private final SeatClient seatClient;
    private final FlightClient flightClient;
    private final PassengerClient passengerClient;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        List<UUID> seatIds = normalizeSeatIds(request.getSeatIds());
        FlightSummaryResponse flight = flightClient.getFlightById(request.getFlightId());
        validateFlightCapacity(flight, seatIds.size());
        List<SeatSummaryResponse> seats = fetchAndValidateSeats(request.getFlightId(), seatIds);
        FareSummaryResponse fareSummary = calculateFare(request, flight, seats);
        String holdReference = generateHoldReference();
        BulkSeatOperationRequest seatOperation = BulkSeatOperationRequest.builder()
                .seatIds(seatIds)
                .holdReference(holdReference)
                .build();

        seatClient.holdSeats(seatOperation);

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .flightId(request.getFlightId())
                .seatIds(new ArrayList<>(seatIds))
                .pnrCode(generatePnr())
                .holdReference(holdReference)
                .tripType(request.getTripType())
                .status(BookingStatus.PENDING)
                .baseFare(fareSummary.getBaseFare())
                .taxes(fareSummary.getTaxes())
                .totalFare(fareSummary.getTotalFare())
                .totalPassengers(fareSummary.getTotalPassengers())
                .mealPreference(request.getMealPreference())
                .luggageKg(request.getLuggageKg())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .paymentId(null)
                .build();

        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            logger.info("Created booking {} with PNR {}", saved.getBookingId(), saved.getPnrCode());
            return mapToResponse(saved);
        } catch (RuntimeException ex) {
            releaseHeldSeatsQuietly(seatIds, holdReference, "booking creation rollback");
            throw ex;
        }
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingOperationException("Only pending bookings can be confirmed");
        }

        flightClient.decrementSeats(booking.getFlightId(), booking.getTotalPassengers());

        try {
            seatClient.confirmSeats(BulkSeatOperationRequest.builder()
                    .seatIds(booking.getSeatIds())
                    .holdReference(booking.getHoldReference())
                    .build());
        } catch (RuntimeException ex) {
            flightClient.incrementSeats(booking.getFlightId(), booking.getTotalPassengers());
            throw ex;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(booking);

        logger.info("Confirmed booking {}", bookingId);
        return mapToResponse(updated);
    }

    @Override
    public BookingResponse getBookingById(UUID bookingId) {
        return mapToResponse(getBookingEntity(bookingId));
    }

    @Override
    public BookingTicketDetailsResponse getBookingTicketDetails(UUID bookingId) {
        Booking booking = getBookingEntity(bookingId);
        FlightSummaryResponse flight = flightClient.getFlightById(booking.getFlightId());
        List<SeatSummaryResponse> seats = seatClient.getSeatsByIds(SeatLookupRequest.builder()
                .seatIds(booking.getSeatIds())
                .build());
        List<PassengerSummaryResponse> passengers = passengerClient.getPassengersByBooking(bookingId);
        Map<UUID, String> seatNumbersById = new HashMap<>();

        for (SeatSummaryResponse seat : seats) {
            seatNumbersById.put(seat.getSeatId(), seat.getSeatNumber());
        }

        List<BookingTicketDetailsResponse.PassengerDetails> passengerDetails = passengers.stream()
                .map(passenger -> BookingTicketDetailsResponse.PassengerDetails.builder()
                        .passengerId(passenger.getPassengerId())
                        .seatId(passenger.getSeatId())
                        .seatNumber(seatNumbersById.getOrDefault(passenger.getSeatId(), "N/A"))
                        .firstName(passenger.getFirstName())
                        .lastName(passenger.getLastName())
                        .fullName(buildPassengerName(passenger.getFirstName(), passenger.getLastName()))
                        .gender(passenger.getGender())
                        .dateOfBirth(passenger.getDateOfBirth())
                        .passportNumber(passenger.getPassportNumber())
                        .nationality(passenger.getNationality())
                        .ticketNumber(passenger.getTicketNumber())
                        .build())
                .toList();

        FareSummaryResponse fareSummary = computeFareSummary(booking.getSeatIds(), flight, seats,
                booking.getMealPreference(), booking.getLuggageKg());

        return BookingTicketDetailsResponse.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUserId())
                .flightId(booking.getFlightId())
                .pnrCode(booking.getPnrCode())
                .tripType(booking.getTripType())
                .status(booking.getStatus())
                .seatIds(List.copyOf(booking.getSeatIds()))
                .totalPassengers(booking.getTotalPassengers())
                .baseFare(fareSummary.getBaseFare())
                .taxes(fareSummary.getTaxes())
                .baggageCharge(fareSummary.getBaggageCharge())
                .mealCharge(fareSummary.getMealCharge())
                .totalFare(fareSummary.getTotalFare())
                .mealPreference(booking.getMealPreference())
                .luggageKg(booking.getLuggageKg())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .bookedAt(booking.getBookedAt())
                .paymentId(booking.getPaymentId())
                .flight(BookingTicketDetailsResponse.FlightDetails.builder()
                        .flightId(flight.getFlightId())
                        .flightNumber(flight.getFlightNumber())
                        .originAirportCode(flight.getOriginAirportCode())
                        .destinationAirportCode(flight.getDestinationAirportCode())
                        .departureTime(flight.getDepartureTime())
                        .arrivalTime(flight.getArrivalTime())
                        .basePrice(flight.getBasePrice())
                        .build())
                .passengers(passengerDetails)
                .build();
    }

    @Override
    public BookingResponse getBookingByPnr(String pnrCode) {
        Booking booking = bookingRepository.findByPnrCode(pnrCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByUser(UUID userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getBookingsByFlight(UUID flightId) {
        return bookingRepository.findByFlightId(flightId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingOperationException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new InvalidBookingOperationException("Completed or no-show booking cannot be cancelled");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            seatClient.releaseSeats(BulkSeatOperationRequest.builder()
                    .seatIds(booking.getSeatIds())
                    .holdReference(booking.getHoldReference())
                    .build());
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            flightClient.incrementSeats(booking.getFlightId(), booking.getTotalPassengers());
            try {
                seatClient.releaseSeats(BulkSeatOperationRequest.builder()
                        .seatIds(booking.getSeatIds())
                        .holdReference(booking.getHoldReference())
                        .build());
            } catch (RuntimeException ex) {
                flightClient.decrementSeats(booking.getFlightId(), booking.getTotalPassengers());
                throw ex;
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);

        logger.info("Cancelled booking {}", bookingId);
        return mapToResponse(updated);
    }

    @Override
    public BookingResponse updateStatus(UUID bookingId, BookingStatusUpdateRequest request) {
        if (request.getStatus() == BookingStatus.CANCELLED) {
            return cancelBooking(bookingId);
        }
        if (request.getStatus() == BookingStatus.CONFIRMED) {
            return confirmBooking(bookingId);
        }

        Booking booking = getBookingEntity(bookingId);
        booking.setStatus(request.getStatus());

        Booking updated = bookingRepository.save(booking);
        logger.info("Updated booking {} status to {}", bookingId, request.getStatus());
        return mapToResponse(updated);
    }

    @Override
    public FareSummaryResponse calculateFare(BookingRequest request) {
        List<UUID> seatIds = normalizeSeatIds(request.getSeatIds());
        FlightSummaryResponse flight = flightClient.getFlightById(request.getFlightId());
        validateFlightCapacity(flight, seatIds.size());
        List<SeatSummaryResponse> seats = fetchAndValidateSeats(request.getFlightId(), seatIds);
        return calculateFare(request, flight, seats);
    }

    @Override
    public BookingResponse addAddOn(UUID bookingId, AddOnRequest request) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingOperationException("Cannot add add-ons to cancelled booking");
        }

        if (request.getMealPreference() != null) {
            booking.setMealPreference(request.getMealPreference());
        }

        if (request.getLuggageKg() != null) {
            booking.setLuggageKg(request.getLuggageKg());
        }

        FlightSummaryResponse flight = flightClient.getFlightById(booking.getFlightId());
        List<SeatSummaryResponse> seats = seatClient.getSeatsByIds(SeatLookupRequest.builder()
                .seatIds(booking.getSeatIds())
                .build());
        FareSummaryResponse fareSummary = computeFareSummary(booking.getSeatIds(), flight, seats,
                booking.getMealPreference(), booking.getLuggageKg());

        booking.setBaseFare(fareSummary.getBaseFare());
        booking.setTaxes(fareSummary.getTaxes());
        booking.setTotalFare(fareSummary.getTotalFare());
        booking.setTotalPassengers(fareSummary.getTotalPassengers());

        Booking updated = bookingRepository.save(booking);
        logger.info("Updated add-ons for booking {}", bookingId);
        return mapToResponse(updated);
    }

    @Override
    public String generatePnr() {
        String pnr;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            pnr = sb.toString();
        } while (bookingRepository.existsByPnrCode(pnr));

        return pnr;
    }

    @Override
    public String generateHoldReference() {
        String ref;
        do {
            ref = "HOLD-" + UUID.randomUUID();
        } while (bookingRepository.existsByHoldReference(ref));
        return ref;
    }

    private Booking getBookingEntity(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUserId())
                .flightId(booking.getFlightId())
                .seatIds(List.copyOf(booking.getSeatIds()))
                .pnrCode(booking.getPnrCode())
                .tripType(booking.getTripType())
                .status(booking.getStatus())
                .baseFare(booking.getBaseFare())
                .taxes(booking.getTaxes())
                .totalFare(booking.getTotalFare())
                .totalPassengers(booking.getTotalPassengers())
                .mealPreference(booking.getMealPreference())
                .luggageKg(booking.getLuggageKg())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .bookedAt(booking.getBookedAt())
                .paymentId(booking.getPaymentId())
                .build();
    }

    private FareSummaryResponse calculateFare(BookingRequest request,
                                              FlightSummaryResponse flight,
                                              List<SeatSummaryResponse> seats) {
        return computeFareSummary(request.getSeatIds(), flight, seats, request.getMealPreference(),
                request.getLuggageKg());
    }

    private FareSummaryResponse computeFareSummary(List<UUID> seatIds,
                                                   FlightSummaryResponse flight,
                                                   List<SeatSummaryResponse> seats,
                                                   String mealPreference,
                                                   Integer luggageKg) {
        BigDecimal baseFare = seats.stream()
                .map(seat -> flight.getBasePrice().multiply(seat.getPriceMultiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxes = baseFare.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baggageCharge = BigDecimal.ZERO;
        BigDecimal mealCharge = BigDecimal.ZERO;

        if (luggageKg != null && luggageKg > 15) {
        	luggageKg -= 15;
            baggageCharge = BAGGAGE_RATE_PER_KG.multiply(BigDecimal.valueOf(luggageKg))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (mealPreference != null
                && !mealPreference.isBlank()
                && !"NONE".equalsIgnoreCase(mealPreference.trim())) {
            mealCharge = MEAL_RATE_PER_PASSENGER.multiply(BigDecimal.valueOf(seatIds.size()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return FareSummaryResponse.builder()
                .seatIds(List.copyOf(seatIds))
                .baseFare(baseFare)
                .taxes(taxes)
                .baggageCharge(baggageCharge)
                .mealCharge(mealCharge)
                .totalFare(baseFare.add(taxes).add(baggageCharge).add(mealCharge).setScale(2, RoundingMode.HALF_UP))
                .totalPassengers(seatIds.size())
                .build();
    }

    private List<SeatSummaryResponse> fetchAndValidateSeats(UUID flightId, List<UUID> seatIds) {
        List<SeatSummaryResponse> seats = seatClient.getSeatsByIds(SeatLookupRequest.builder()
                .seatIds(seatIds)
                .build());

        if (seats.size() != seatIds.size()) {
            throw new InvalidBookingOperationException("One or more selected seats were not found");
        }

        for (SeatSummaryResponse seat : seats) {
            if (!flightId.equals(seat.getFlightId())) {
                throw new InvalidBookingOperationException("All seats must belong to the selected flight");
            }
        }

        return seats;
    }

    private List<UUID> normalizeSeatIds(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new InvalidBookingOperationException("At least one seat must be selected");
        }

        Set<UUID> uniqueSeatIds = new HashSet<>(seatIds);
        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new InvalidBookingOperationException("Duplicate seats are not allowed in a booking");
        }

        return List.copyOf(seatIds);
    }

    private void releaseHeldSeatsQuietly(List<UUID> seatIds, String holdReference, String reason) {
        try {
            seatClient.releaseSeats(BulkSeatOperationRequest.builder()
                    .seatIds(seatIds)
                    .holdReference(holdReference)
                    .build());
        } catch (RuntimeException releaseEx) {
            logger.error("Failed to release seats during {}: {}", reason, releaseEx.getMessage());
        }
    }

    private String buildPassengerName(String firstName, String lastName) {
        String safeFirstName = firstName == null ? "" : firstName.trim();
        String safeLastName = lastName == null ? "" : lastName.trim();
        return (safeFirstName + " " + safeLastName).trim();
    }

    private void validateFlightCapacity(FlightSummaryResponse flight, int totalPassengers) {
        if (flight.getAvailableSeats() != null && flight.getAvailableSeats() < totalPassengers) {
            throw new InvalidBookingOperationException("Not enough seats available for this booking");
        }
    }
}
