package com.skybooker.booking.service;

import com.skybooker.booking.client.FlightClient;
import com.skybooker.booking.client.PassengerClient;
import com.skybooker.booking.client.SeatClient;
import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.dto.BookingStatusUpdateRequest;
import com.skybooker.booking.dto.FareSummaryResponse;
import com.skybooker.booking.dto.external.BulkSeatOperationRequest;
import com.skybooker.booking.dto.external.FlightSummaryResponse;
import com.skybooker.booking.dto.external.SeatSummaryResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.entity.TripType;
import com.skybooker.booking.exception.InvalidBookingOperationException;
import com.skybooker.booking.exception.ResourceNotFoundException;
import com.skybooker.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatClient seatClient;

    @Mock
    private FlightClient flightClient;

    @Mock
    private PassengerClient passengerClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID bookingId;
    private UUID userId;
    private UUID flightId;
    private List<UUID> seatIds;
    private FlightSummaryResponse flight;
    private List<SeatSummaryResponse> seats;
    private Booking booking;
    private BookingRequest request;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        seatIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        request = new BookingRequest();
        request.setUserId(userId);
        request.setFlightId(flightId);
        request.setSeatIds(seatIds);
        request.setTripType(TripType.ONE_WAY);
        request.setMealPreference("VEG");
        request.setLuggageKg(10);
        request.setContactEmail("himanshu@example.com");
        request.setContactPhone("9876543210");

        flight = new FlightSummaryResponse();
        flight.setFlightId(flightId);
        flight.setBasePrice(BigDecimal.valueOf(5000));
        flight.setAvailableSeats(20);

        seats = List.of(
                seat(seatIds.get(0), "12A", BigDecimal.ONE),
                seat(seatIds.get(1), "12B", BigDecimal.valueOf(1.2))
        );

        booking = Booking.builder()
                .bookingId(bookingId)
                .userId(userId)
                .flightId(flightId)
                .seatIds(seatIds)
                .pnrCode("ABC123")
                .holdReference("HOLD-123")
                .tripType(TripType.ONE_WAY)
                .status(BookingStatus.PENDING)
                .baseFare(BigDecimal.valueOf(11000))
                .taxes(BigDecimal.valueOf(1980))
                .totalFare(BigDecimal.valueOf(15480))
                .totalPassengers(seatIds.size())
                .mealPreference("VEG")
                .luggageKg(10)
                .contactEmail("himanshu@example.com")
                .contactPhone("9876543210")
                .bookedAt(LocalDateTime.now())
                .paymentId(null)
                .build();
    }

    @Test
    void createBooking_success() {
        when(bookingRepository.existsByHoldReference(anyString())).thenReturn(false);
        when(bookingRepository.existsByPnrCode(anyString())).thenReturn(false);
        when(flightClient.getFlightById(flightId)).thenReturn(flight);
        when(seatClient.getSeatsByIds(any())).thenReturn(seats);
        when(seatClient.holdSeats(any(BulkSeatOperationRequest.class))).thenReturn(seats);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setBookingId(bookingId);
            saved.setBookedAt(LocalDateTime.now());
            return saved;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(userId, response.getUserId());
        assertEquals(flightId, response.getFlightId());
        assertEquals(seatIds, response.getSeatIds());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(2, response.getTotalPassengers());
        assertEquals(0, BigDecimal.valueOf(15480).setScale(2).compareTo(response.getTotalFare()));

        verify(flightClient).getFlightById(flightId);
        verify(seatClient).getSeatsByIds(any());
        verify(seatClient).holdSeats(any(BulkSeatOperationRequest.class));
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void createBooking_shouldHoldAllSeatsWithGeneratedReference() {
        when(bookingRepository.existsByHoldReference(anyString())).thenReturn(false);
        when(bookingRepository.existsByPnrCode(anyString())).thenReturn(false);
        when(flightClient.getFlightById(flightId)).thenReturn(flight);
        when(seatClient.getSeatsByIds(any())).thenReturn(seats);
        when(seatClient.holdSeats(any(BulkSeatOperationRequest.class))).thenReturn(seats);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setBookingId(bookingId);
            return saved;
        });

        bookingService.createBooking(request);

        ArgumentCaptor<BulkSeatOperationRequest> captor = ArgumentCaptor.forClass(BulkSeatOperationRequest.class);
        verify(seatClient).holdSeats(captor.capture());

        assertEquals(seatIds, captor.getValue().getSeatIds());
        assertNotNull(captor.getValue().getHoldReference());
        assertTrue(captor.getValue().getHoldReference().startsWith("HOLD-"));
    }

    @Test
    void confirmBooking_success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(seatClient.confirmSeats(any(BulkSeatOperationRequest.class))).thenReturn(seats);
        when(flightClient.decrementSeats(flightId, 2)).thenReturn(new Object());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.confirmBooking(bookingId);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(seatClient).confirmSeats(any(BulkSeatOperationRequest.class));
        verify(flightClient).decrementSeats(flightId, 2);
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_whenNotPending_throwException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvalidBookingOperationException exception = assertThrows(
                InvalidBookingOperationException.class,
                () -> bookingService.confirmBooking(bookingId)
        );

        assertEquals("Only pending bookings can be confirmed", exception.getMessage());
        verify(seatClient, never()).confirmSeats(any());
        verify(flightClient, never()).decrementSeats(any(), anyInt());
    }

    @Test
    void getBookingById_success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBookingById(bookingId);

        assertEquals(bookingId, response.getBookingId());
        assertEquals("ABC123", response.getPnrCode());
        assertEquals(seatIds, response.getSeatIds());
    }

    @Test
    void getBookingById_whenNotFound_throwException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBookingById(bookingId));
    }

    @Test
    void getBookingByPnr_success() {
        when(bookingRepository.findByPnrCode("ABC123")).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBookingByPnr("abc123");

        assertEquals("ABC123", response.getPnrCode());
        verify(bookingRepository).findByPnrCode("ABC123");
    }

    @Test
    void getBookingByPnr_whenNotFound_throwException() {
        when(bookingRepository.findByPnrCode("ABC123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBookingByPnr("ABC123"));
    }

    @Test
    void getBookingsByUser_success() {
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsByUser(userId);

        assertEquals(1, responses.size());
        assertEquals(userId, responses.get(0).getUserId());
    }

    @Test
    void getBookingsByFlight_success() {
        when(bookingRepository.findByFlightId(flightId)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsByFlight(flightId);

        assertEquals(1, responses.size());
        assertEquals(flightId, responses.get(0).getFlightId());
    }

    @Test
    void cancelBooking_whenPending_success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(seatClient.releaseSeats(any(BulkSeatOperationRequest.class))).thenReturn(seats);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(seatClient).releaseSeats(any(BulkSeatOperationRequest.class));
        verify(flightClient, never()).incrementSeats(any(), anyInt());
    }

    @Test
    void cancelBooking_whenConfirmed_success() {
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(flightClient.incrementSeats(flightId, 2)).thenReturn(new Object());
        when(seatClient.releaseSeats(any(BulkSeatOperationRequest.class))).thenReturn(seats);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(flightClient).incrementSeats(flightId, 2);
        verify(seatClient).releaseSeats(any(BulkSeatOperationRequest.class));
    }

    @Test
    void cancelBooking_whenAlreadyCancelled_throwException() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvalidBookingOperationException exception = assertThrows(
                InvalidBookingOperationException.class,
                () -> bookingService.cancelBooking(bookingId)
        );

        assertEquals("Booking is already cancelled", exception.getMessage());
    }

    @Test
    void cancelBooking_whenCompleted_throwException() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvalidBookingOperationException exception = assertThrows(
                InvalidBookingOperationException.class,
                () -> bookingService.cancelBooking(bookingId)
        );

        assertEquals("Completed or no-show booking cannot be cancelled", exception.getMessage());
    }

    @Test
    void cancelBooking_whenNoShow_throwException() {
        booking.setStatus(BookingStatus.NO_SHOW);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvalidBookingOperationException exception = assertThrows(
                InvalidBookingOperationException.class,
                () -> bookingService.cancelBooking(bookingId)
        );

        assertEquals("Completed or no-show booking cannot be cancelled", exception.getMessage());
    }

    @Test
    void updateStatus_success() {
        BookingStatusUpdateRequest statusRequest = new BookingStatusUpdateRequest();
        statusRequest.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.updateStatus(bookingId, statusRequest);

        assertEquals(BookingStatus.COMPLETED, response.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void calculateFare_withMealAndLuggage_success() {
        when(flightClient.getFlightById(flightId)).thenReturn(flight);
        when(seatClient.getSeatsByIds(any())).thenReturn(seats);

        FareSummaryResponse response = bookingService.calculateFare(request);

        assertEquals(BigDecimal.valueOf(11000).setScale(2), response.getBaseFare());
        assertEquals(BigDecimal.valueOf(1980).setScale(2), response.getTaxes());
        assertEquals(BigDecimal.valueOf(2000).setScale(2), response.getBaggageCharge());
        assertEquals(BigDecimal.valueOf(500).setScale(2), response.getMealCharge());
        assertEquals(BigDecimal.valueOf(15480).setScale(2), response.getTotalFare());
        assertEquals(2, response.getTotalPassengers());
    }

    @Test
    void calculateFare_withoutMealAndLuggage_success() {
        request.setMealPreference(null);
        request.setLuggageKg(0);
        when(flightClient.getFlightById(flightId)).thenReturn(flight);
        when(seatClient.getSeatsByIds(any())).thenReturn(seats);

        FareSummaryResponse response = bookingService.calculateFare(request);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.getBaggageCharge()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getMealCharge()));
        assertEquals(0, BigDecimal.valueOf(12980).setScale(2).compareTo(response.getTotalFare()));
    }

    @Test
    void addAddOn_success() {
        AddOnRequest addOnRequest = new AddOnRequest();
        addOnRequest.setMealPreference("NON_VEG");
        addOnRequest.setLuggageKg(5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(flightClient.getFlightById(flightId)).thenReturn(flight);
        when(seatClient.getSeatsByIds(any())).thenReturn(seats);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.addAddOn(bookingId, addOnRequest);

        assertEquals("NON_VEG", response.getMealPreference());
        assertEquals(5, response.getLuggageKg());
        assertEquals(BigDecimal.valueOf(14480).setScale(2), response.getTotalFare());
    }

    @Test
    void addAddOn_whenCancelled_throwException() {
        booking.setStatus(BookingStatus.CANCELLED);

        AddOnRequest addOnRequest = new AddOnRequest();
        addOnRequest.setMealPreference("VEG");
        addOnRequest.setLuggageKg(5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        InvalidBookingOperationException exception = assertThrows(
                InvalidBookingOperationException.class,
                () -> bookingService.addAddOn(bookingId, addOnRequest)
        );

        assertEquals("Cannot add add-ons to cancelled booking", exception.getMessage());
    }

    @Test
    void generatePnr_success() {
        when(bookingRepository.existsByPnrCode(anyString())).thenReturn(false);

        String pnr = bookingService.generatePnr();

        assertNotNull(pnr);
        assertEquals(6, pnr.length());
        verify(bookingRepository, atLeastOnce()).existsByPnrCode(anyString());
    }

    @Test
    void generateHoldReference_success() {
        when(bookingRepository.existsByHoldReference(anyString())).thenReturn(false);

        String holdReference = bookingService.generateHoldReference();

        assertNotNull(holdReference);
        assertTrue(holdReference.startsWith("HOLD-"));
        verify(bookingRepository, atLeastOnce()).existsByHoldReference(anyString());
    }

    private SeatSummaryResponse seat(UUID seatId, String seatNumber, BigDecimal multiplier) {
        SeatSummaryResponse seat = new SeatSummaryResponse();
        seat.setSeatId(seatId);
        seat.setFlightId(flightId);
        seat.setSeatNumber(seatNumber);
        seat.setPriceMultiplier(multiplier);
        seat.setStatus("AVAILABLE");
        return seat;
    }
}
