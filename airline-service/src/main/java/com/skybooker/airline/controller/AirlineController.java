package com.skybooker.airline.controller;

import com.skybooker.airline.dto.*;
import com.skybooker.airline.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @PostMapping("/airlines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponse> createAirline(@Valid @RequestBody AirlineRequest request) {
        return new ResponseEntity<>(airlineService.createAirline(request), HttpStatus.CREATED);
    }

    @GetMapping("/airlines")
    public ResponseEntity<List<AirlineResponse>> getAllAirlines() {
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    @GetMapping("/airlines/active")
    public ResponseEntity<List<AirlineResponse>> getActiveAirlines() {
        return ResponseEntity.ok(airlineService.getActiveAirlines());
    }

    @GetMapping("/airlines/{airlineId}")
    public ResponseEntity<AirlineResponse> getAirlineById(@PathVariable UUID airlineId) {
        return ResponseEntity.ok(airlineService.getAirlineById(airlineId));
    }

    @GetMapping("/airlines/iata/{iataCode}")
    public ResponseEntity<AirlineResponse> getAirlineByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirlineByIata(iataCode));
    }

    @PutMapping("/airlines/{airlineId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponse> updateAirline(@PathVariable UUID airlineId,
                                                         @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.updateAirline(airlineId, request));
    }

    @PutMapping("/airlines/{airlineId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponse> deactivateAirline(@PathVariable UUID airlineId) {
        return ResponseEntity.ok(airlineService.deactivateAirline(airlineId));
    }

    @PostMapping("/airports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request) {
        return new ResponseEntity<>(airlineService.createAirport(request), HttpStatus.CREATED);
    }

    @GetMapping("/airports/iata/{iataCode}")
    public ResponseEntity<AirportResponse> getAirportByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirportByIata(iataCode));
    }

    @GetMapping("/airports/search")
    public ResponseEntity<List<AirportResponse>> searchAirports(@RequestParam String keyword) {
        return ResponseEntity.ok(airlineService.searchAirports(keyword));
    }

    @GetMapping("/airports/city/{city}")
    public ResponseEntity<List<AirportResponse>> getAirportsByCity(@PathVariable String city) {
        return ResponseEntity.ok(airlineService.getAirportsByCity(city));
    }

    @GetMapping("/airports/country/{country}")
    public ResponseEntity<List<AirportResponse>> getAirportsByCountry(@PathVariable String country) {
        return ResponseEntity.ok(airlineService.getAirportsByCountry(country));
    }

    @PutMapping("/airports/{airportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirportResponse> updateAirport(@PathVariable UUID airportId,
                                                         @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airlineService.updateAirport(airportId, request));
    }
}