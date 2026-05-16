package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.exception.FlightAlreadyExistsException;
import com.skybooker.flight.exception.InvalidFlightDataException;
import com.skybooker.flight.exception.ResourceNotFoundException;
import com.skybooker.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    @Override
    public FlightResponse addFlight(FlightRequest request) {
    	
    	if(flightRepository.existsByFlightNumber(request.getFlightNumber())) {
    		 throw new FlightAlreadyExistsException("Flight number " + request.getFlightNumber()+" already exists");
    	}
    	
    	validateFlightRequest(request);
    	
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber().toUpperCase())
                .airlineId(request.getAirlineId())
                .originAirportCode(request.getOriginAirportCode().toUpperCase())
                .destinationAirportCode(request.getDestinationAirportCode().toUpperCase())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .durationMinutes((int) Duration.between(
                        request.getDepartureTime(),
                        request.getArrivalTime()
                ).toMinutes())
                .status(FlightStatus.ON_TIME)
                .aircraftType(request.getAircraftType())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .basePrice(request.getBasePrice())
                .build();

        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse getFlightById(UUID flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        return mapToResponse(flight);
    }

    @Override
    public FlightResponse getFlightByNumber(String flightNumber) {
        Flight flight = flightRepository.findByFlightNumber(flightNumber.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        return mapToResponse(flight);
    }

    @Override
    public List<FlightResponse> getFlightsByAirline(UUID airlineId) {
        return flightRepository.findByAirlineId(airlineId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<FlightResponse> searchFlights(String origin, String destination, LocalDate departureDate) {
        LocalDateTime start = departureDate.atStartOfDay();
        LocalDateTime end = departureDate.plusDays(1).atStartOfDay().minusNanos(1);

        return flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetweenAndStatusNot(
                        origin.toUpperCase(),
                        destination.toUpperCase(),
                        start,
                        end,
                        FlightStatus.CANCELLED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public FlightResponse updateFlight(UUID flightId, FlightRequest request) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        
        if (request.getTotalSeats() < (flight.getTotalSeats() - flight.getAvailableSeats())) {
            throw new InvalidFlightDataException("Total seats cannot be less than already booked seats");
        }
        
        validateFlightRequest(request);

        flight.setFlightNumber(request.getFlightNumber().toUpperCase());
        flight.setAirlineId(request.getAirlineId());
        flight.setOriginAirportCode(request.getOriginAirportCode().toUpperCase());
        flight.setDestinationAirportCode(request.getDestinationAirportCode().toUpperCase());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setDurationMinutes((int) Duration.between(
                request.getDepartureTime(),
                request.getArrivalTime()
        ).toMinutes());
        flight.setAircraftType(request.getAircraftType());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setBasePrice(request.getBasePrice());

        if (flight.getAvailableSeats() > flight.getTotalSeats()) {
            flight.setAvailableSeats(flight.getTotalSeats());
        }

        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse updateStatus(UUID flightId, FlightStatus status) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        flight.setStatus(status);
        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public void deleteFlight(UUID flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        flightRepository.delete(flight);
    }

    @Override
    public FlightResponse decrementSeats(UUID flightId, int count) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        if (count <= 0) {
            throw new InvalidFlightDataException("Count must be greater than 0");
        }

        if (flight.getAvailableSeats() < count) {
            throw new InvalidFlightDataException("Not enough available seats");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - count);

        //logger.info("Decremented {} seat(s) for flight {}", count, flightId);
        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse incrementSeats(UUID flightId, int count) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        if (count <= 0) {
            throw new InvalidFlightDataException("Count must be greater than 0");
        }

        int updatedSeats = flight.getAvailableSeats() + count;
        if (updatedSeats > flight.getTotalSeats()) {
            updatedSeats = flight.getTotalSeats();
        }

        flight.setAvailableSeats(updatedSeats);

        //logger.info("Incremented {} seat(s) for flight {}", count, flightId);
        return mapToResponse(flightRepository.save(flight));
    }
    
    @Override
    public List<FlightResponse> getAllFlights(){
    	
    	return flightRepository.findAll().stream().map(this::mapToResponse).toList();
    }
    
    private void validateFlightRequest(FlightRequest request) {
        if (request.getOriginAirportCode().equalsIgnoreCase(request.getDestinationAirportCode())) {
            throw new InvalidFlightDataException("Origin and destination airport cannot be the same");
        }

        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new InvalidFlightDataException("Arrival time must be after departure time");
        }

        if (request.getTotalSeats() == null || request.getTotalSeats() <= 0) {
            throw new InvalidFlightDataException("Total seats must be greater than 0");
        }

        if (request.getBasePrice() == null || request.getBasePrice().signum() <= 0) {
            throw new InvalidFlightDataException("Base price must be greater than 0");
        }
    }
    
    private FlightResponse mapToResponse(Flight flight) {
        return FlightResponse.builder()
                .flightId(flight.getFlightId())
                .flightNumber(flight.getFlightNumber())
                .airlineId(flight.getAirlineId())
                .originAirportCode(flight.getOriginAirportCode())
                .destinationAirportCode(flight.getDestinationAirportCode())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .durationMinutes(flight.getDurationMinutes())
                .status(flight.getStatus())
                .aircraftType(flight.getAircraftType())
                .totalSeats(flight.getTotalSeats())
                .availableSeats(flight.getAvailableSeats())
                .basePrice(flight.getBasePrice())
                .build();
    }
    
}