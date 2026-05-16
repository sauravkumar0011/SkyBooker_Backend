package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.FlightStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FlightService {

    FlightResponse addFlight(FlightRequest request);

    FlightResponse getFlightById(UUID flightId);

    FlightResponse getFlightByNumber(String flightNumber);

    List<FlightResponse> getFlightsByAirline(UUID airlineId);

    List<FlightResponse> searchFlights(String origin, String destination, LocalDate departureDate);

    List<FlightResponse> getAllFlights();
    
    FlightResponse updateFlight(UUID flightId, FlightRequest request);

    FlightResponse updateStatus(UUID flightId, FlightStatus status);

    FlightResponse decrementSeats(UUID flightId, int count);

    FlightResponse incrementSeats(UUID flightId, int count);
    
    void deleteFlight(UUID flightId);
    
}