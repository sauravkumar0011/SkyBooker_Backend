package com.skybooker.passenger.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.PassengerUpdateRequest;
import com.skybooker.passenger.service.PassengerService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<List<PassengerResponse>> addBulk(@Valid @RequestBody PassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passengerService.addPassengers(request));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PassengerResponse>> getByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(passengerService.getByBooking(bookingId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(passengerService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<PassengerResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody PassengerUpdateRequest request) {
        return ResponseEntity.ok(passengerService.updatePassenger(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        passengerService.deletePassenger(id);
        return ResponseEntity.ok("Deleted");
    }
}
