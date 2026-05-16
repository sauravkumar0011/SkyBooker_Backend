package com.skybooker.flight.controller;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.dto.UpdateFlightStatusRequest;
import com.skybooker.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @PreAuthorize("hasRole('AIRLINE_STAFF')")
    public ResponseEntity<FlightResponse> addFlight(@Valid @RequestBody FlightRequest request) {
        return new ResponseEntity<>(flightService.addFlight(request), HttpStatus.CREATED);
    }

    @GetMapping
    public List<FlightResponse> getAllFlights() {
        return flightService.getAllFlights();
    }
    
    @GetMapping("/{flightId}")
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable UUID flightId) {
        return ResponseEntity.ok(flightService.getFlightById(flightId));
    }

    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlightByNumber(@PathVariable String flightNumber) {
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<FlightResponse>> getFlightsByAirline(@PathVariable UUID airlineId) {
        return ResponseEntity.ok(flightService.getFlightsByAirline(airlineId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FlightResponse>> searchFlights(@RequestParam String origin,
                                                              @RequestParam String destination,
                                                              @RequestParam LocalDate departureDate) {
        return ResponseEntity.ok(flightService.searchFlights(origin, destination, departureDate));
    }

    @PutMapping("/{flightId}")
    @PreAuthorize("hasRole('AIRLINE_STAFF')")
    public ResponseEntity<FlightResponse> updateFlight(@PathVariable UUID flightId,
                                                       @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.updateFlight(flightId, request));
    }

    @PutMapping("/{flightId}/status")
    @PreAuthorize("hasRole('AIRLINE_STAFF')")
    public ResponseEntity<FlightResponse> updateStatus(@PathVariable UUID flightId,
                                                       @Valid @RequestBody UpdateFlightStatusRequest request) {
        return ResponseEntity.ok(flightService.updateStatus(flightId, request.getStatus()));
    }

    @PutMapping("/{flightId}/decrement-seats")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<FlightResponse> decrementSeats(@PathVariable UUID flightId,
                                                         @RequestParam int count) {
        return ResponseEntity.ok(flightService.decrementSeats(flightId, count));
    }

    @PutMapping("/{flightId}/increment-seats")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<FlightResponse> incrementSeats(@PathVariable UUID flightId,
                                                         @RequestParam int count) {
        return ResponseEntity.ok(flightService.incrementSeats(flightId, count));
    }
    
    @DeleteMapping("/{flightId}")
    @PreAuthorize("hasRole('AIRLINE_STAFF')")
    public ResponseEntity<String> deleteFlight(@PathVariable UUID flightId) {
        flightService.deleteFlight(flightId);
        return ResponseEntity.ok("Flight deleted successfully");
    }
}