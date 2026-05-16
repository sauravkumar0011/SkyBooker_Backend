package com.skybooker.seat.controller;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF')")
    public ResponseEntity<SeatResponse> addSeat(@Valid @RequestBody SeatRequest request) {
        return new ResponseEntity<>(seatService.addSeat(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF')")
    public ResponseEntity<List<SeatResponse>> addSeatsInBulk(@Valid @RequestBody BulkSeatRequest request) {
        return new ResponseEntity<>(seatService.addSeatsInBulk(request), HttpStatus.CREATED);
    }

    @GetMapping("/flight/{flightId}/map")
    public ResponseEntity<List<SeatResponse>> getSeatMap(@PathVariable UUID flightId) {
        return ResponseEntity.ok(seatService.getSeatMap(flightId));
    }

    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable UUID flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeats(flightId));
    }

    @GetMapping("/flight/{flightId}/available/{seatClass}")
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByClass(@PathVariable UUID flightId,
                                                                       @PathVariable SeatClass seatClass) {
        return ResponseEntity.ok(seatService.getAvailableSeatsByClass(flightId, seatClass));
    }

    @PostMapping("/details/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<List<SeatResponse>> getSeatsByIds(@Valid @RequestBody SeatLookupRequest request) {
        return ResponseEntity.ok(seatService.getSeatsByIds(request.getSeatIds()));
    }

    @PutMapping("/{seatId}/hold")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<SeatResponse> holdSeat(@PathVariable UUID seatId,
                                                 @Valid @RequestBody HoldSeatRequest request) {
        return ResponseEntity.ok(seatService.holdSeat(seatId, request.getHoldReference()));
    }

    @PutMapping("/{seatId}/release")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<SeatResponse> releaseSeat(@PathVariable UUID seatId,
                                                    @Valid @RequestBody ReleaseSeatRequest request) {
        return ResponseEntity.ok(seatService.releaseSeat(seatId, request.getHoldReference()));
    }

    @PutMapping("/{seatId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<SeatResponse> confirmSeat(@PathVariable UUID seatId,
                                                    @Valid @RequestBody ConfirmSeatRequest request) {
        return ResponseEntity.ok(seatService.confirmSeat(seatId, request.getHoldReference()));
    }

    @PostMapping("/hold/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<List<SeatResponse>> holdSeatsInBulk(@Valid @RequestBody BulkSeatOperationRequest request) {
        return ResponseEntity.ok(seatService.holdSeatsInBulk(request.getSeatIds(), request.getHoldReference()));
    }

    @PostMapping("/release/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<List<SeatResponse>> releaseSeatsInBulk(@Valid @RequestBody BulkSeatOperationRequest request) {
        return ResponseEntity.ok(seatService.releaseSeatsInBulk(request.getSeatIds(), request.getHoldReference()));
    }

    @PostMapping("/confirm/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF','PASSENGER')")
    public ResponseEntity<List<SeatResponse>> confirmSeatsInBulk(@Valid @RequestBody BulkSeatOperationRequest request) {
        return ResponseEntity.ok(seatService.confirmSeatsInBulk(request.getSeatIds(), request.getHoldReference()));
    }

    @GetMapping("/flight/{flightId}/count/{seatClass}")
    public ResponseEntity<SeatCountResponse> countAvailableByClass(@PathVariable UUID flightId,
                                                                   @PathVariable SeatClass seatClass) {
        return ResponseEntity.ok(seatService.countAvailableByClass(flightId, seatClass));
    }

    @DeleteMapping("/flight/{flightId}")
    @PreAuthorize("hasRole('AIRLINE_STAFF')")
    public ResponseEntity<String> deleteSeatsForFlight(@PathVariable UUID flightId) {
        seatService.deleteSeatsForFlight(flightId);
        return ResponseEntity.ok("Seats deleted successfully for flight");
    }
}
