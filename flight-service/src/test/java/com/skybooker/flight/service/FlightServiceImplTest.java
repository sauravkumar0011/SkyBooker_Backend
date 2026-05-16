package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.exception.FlightAlreadyExistsException;
import com.skybooker.flight.exception.InvalidFlightDataException;
import com.skybooker.flight.exception.ResourceNotFoundException;
import com.skybooker.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightServiceImpl flightService;

    private UUID flightId;
    private UUID airlineId;
    private Flight flight;
    private FlightRequest request;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        airlineId = UUID.randomUUID();

        request = new FlightRequest();
        request.setFlightNumber("AI101");
        request.setAirlineId(airlineId);
        request.setOriginAirportCode("del");
        request.setDestinationAirportCode("bom");
        request.setDepartureTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        request.setArrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0));
        request.setAircraftType("Airbus A320");
        request.setTotalSeats(180);
        request.setBasePrice(BigDecimal.valueOf(5500));

        flight = Flight.builder()
                .flightId(flightId)
                .flightNumber("AI101")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .durationMinutes(120)
                .status(FlightStatus.ON_TIME)
                .aircraftType("Airbus A320")
                .totalSeats(180)
                .availableSeats(180)
                .basePrice(BigDecimal.valueOf(5500))
                .build();
    }

    @Test
    void addFlight_success() {
        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(false);
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        FlightResponse response = flightService.addFlight(request);

        assertNotNull(response);
        assertEquals(flightId, response.getFlightId());
        assertEquals("AI101", response.getFlightNumber());
        assertEquals("DEL", response.getOriginAirportCode());
        assertEquals("BOM", response.getDestinationAirportCode());
        assertEquals(FlightStatus.ON_TIME, response.getStatus());
        assertEquals(180, response.getTotalSeats());
        assertEquals(180, response.getAvailableSeats());

        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void addFlight_whenFlightNumberExists_throwException() {
        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(true);

        FlightAlreadyExistsException exception = assertThrows(
                FlightAlreadyExistsException.class,
                () -> flightService.addFlight(request)
        );

        assertEquals("Flight number AI101 already exists", exception.getMessage());
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void addFlight_whenOriginAndDestinationSame_throwException() {
        request.setDestinationAirportCode("DEL");

        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(false);

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.addFlight(request)
        );

        assertEquals("Origin and destination airport cannot be the same", exception.getMessage());
    }

    @Test
    void addFlight_whenArrivalBeforeDeparture_throwException() {
        request.setArrivalTime(LocalDateTime.of(2026, 5, 10, 9, 0));

        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(false);

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.addFlight(request)
        );

        assertEquals("Arrival time must be after departure time", exception.getMessage());
    }

    @Test
    void addFlight_whenTotalSeatsInvalid_throwException() {
        request.setTotalSeats(0);

        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(false);

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.addFlight(request)
        );

        assertEquals("Total seats must be greater than 0", exception.getMessage());
    }

    @Test
    void addFlight_whenBasePriceInvalid_throwException() {
        request.setBasePrice(BigDecimal.ZERO);

        when(flightRepository.existsByFlightNumber("AI101")).thenReturn(false);

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.addFlight(request)
        );

        assertEquals("Base price must be greater than 0", exception.getMessage());
    }

    @Test
    void getFlightById_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        FlightResponse response = flightService.getFlightById(flightId);

        assertEquals(flightId, response.getFlightId());
        assertEquals("AI101", response.getFlightNumber());
    }

    @Test
    void getFlightById_whenNotFound_throwException() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> flightService.getFlightById(flightId));
    }

    @Test
    void getFlightByNumber_success() {
        when(flightRepository.findByFlightNumber("AI101")).thenReturn(Optional.of(flight));

        FlightResponse response = flightService.getFlightByNumber("ai101");

        assertEquals("AI101", response.getFlightNumber());
        verify(flightRepository).findByFlightNumber("AI101");
    }

    @Test
    void getFlightByNumber_whenNotFound_throwException() {
        when(flightRepository.findByFlightNumber("AI101")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> flightService.getFlightByNumber("AI101"));
    }

    @Test
    void getFlightsByAirline_success() {
        when(flightRepository.findByAirlineId(airlineId)).thenReturn(List.of(flight));

        List<FlightResponse> response = flightService.getFlightsByAirline(airlineId);

        assertEquals(1, response.size());
        assertEquals(airlineId, response.get(0).getAirlineId());
    }

    @Test
    void searchFlights_success() {
        LocalDate date = LocalDate.of(2026, 5, 10);

        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetweenAndStatusNot(
                eq("DEL"),
                eq("BOM"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(FlightStatus.CANCELLED)
        )).thenReturn(List.of(flight));

        List<FlightResponse> response = flightService.searchFlights("del", "bom", date);

        assertEquals(1, response.size());
        assertEquals("DEL", response.get(0).getOriginAirportCode());
        assertEquals("BOM", response.get(0).getDestinationAirportCode());
    }

    @Test
    void updateFlight_success() {
        FlightRequest updateRequest = new FlightRequest();
        updateRequest.setFlightNumber("AI202");
        updateRequest.setAirlineId(airlineId);
        updateRequest.setOriginAirportCode("ccu");
        updateRequest.setDestinationAirportCode("hyd");
        updateRequest.setDepartureTime(LocalDateTime.of(2026, 5, 11, 8, 0));
        updateRequest.setArrivalTime(LocalDateTime.of(2026, 5, 11, 10, 30));
        updateRequest.setAircraftType("Boeing 737");
        updateRequest.setTotalSeats(200);
        updateRequest.setBasePrice(BigDecimal.valueOf(6500));

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = flightService.updateFlight(flightId, updateRequest);

        assertEquals("AI202", response.getFlightNumber());
        assertEquals("CCU", response.getOriginAirportCode());
        assertEquals("HYD", response.getDestinationAirportCode());
        assertEquals(150, response.getDurationMinutes());
        assertEquals(200, response.getTotalSeats());

        verify(flightRepository).save(flight);
    }

    @Test
    void updateFlight_whenNotFound_throwException() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> flightService.updateFlight(flightId, request));
    }

    @Test
    void updateFlight_whenTotalSeatsLessThanBookedSeats_throwException() {
        flight.setTotalSeats(180);
        flight.setAvailableSeats(100);

        request.setTotalSeats(50);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.updateFlight(flightId, request)
        );

        assertEquals("Total seats cannot be less than already booked seats", exception.getMessage());
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void updateStatus_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = flightService.updateStatus(flightId, FlightStatus.DELAYED);

        assertEquals(FlightStatus.DELAYED, response.getStatus());
        verify(flightRepository).save(flight);
    }

    @Test
    void deleteFlight_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        flightService.deleteFlight(flightId);

        verify(flightRepository).delete(flight);
    }

    @Test
    void deleteFlight_whenNotFound_throwException() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> flightService.deleteFlight(flightId));

        verify(flightRepository, never()).delete(any(Flight.class));
    }

    @Test
    void decrementSeats_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = flightService.decrementSeats(flightId, 2);

        assertEquals(178, response.getAvailableSeats());
        verify(flightRepository).save(flight);
    }

    @Test
    void decrementSeats_whenCountInvalid_throwException() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.decrementSeats(flightId, 0)
        );

        assertEquals("Count must be greater than 0", exception.getMessage());
    }

    @Test
    void decrementSeats_whenNotEnoughSeats_throwException() {
        flight.setAvailableSeats(1);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.decrementSeats(flightId, 2)
        );

        assertEquals("Not enough available seats", exception.getMessage());
    }

    @Test
    void incrementSeats_success() {
        flight.setAvailableSeats(100);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = flightService.incrementSeats(flightId, 10);

        assertEquals(110, response.getAvailableSeats());
        verify(flightRepository).save(flight);
    }

    @Test
    void incrementSeats_whenMoreThanTotalSeats_shouldSetToTotalSeats() {
        flight.setAvailableSeats(179);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = flightService.incrementSeats(flightId, 10);

        assertEquals(180, response.getAvailableSeats());
    }

    @Test
    void incrementSeats_whenCountInvalid_throwException() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));

        InvalidFlightDataException exception = assertThrows(
                InvalidFlightDataException.class,
                () -> flightService.incrementSeats(flightId, -1)
        );

        assertEquals("Count must be greater than 0", exception.getMessage());
    }

    @Test
    void getAllFlights_success() {
        when(flightRepository.findAll()).thenReturn(List.of(flight));

        List<FlightResponse> response = flightService.getAllFlights();

        assertEquals(1, response.size());
        assertEquals("AI101", response.get(0).getFlightNumber());
    }
}