package com.skybooker.booking.controller;

import com.skybooker.booking.dto.*;
import com.skybooker.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return new ResponseEntity<>(bookingService.createBooking(request), HttpStatus.CREATED);
    }

    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @GetMapping("/{bookingId}/ticket-details")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER','AIRLINE_STAFF')")
    public ResponseEntity<BookingTicketDetailsResponse> getBookingTicketDetails(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBookingTicketDetails(bookingId));
    }

    @GetMapping("/pnr/{pnrCode}")
    public ResponseEntity<BookingResponse> getBookingByPnr(@PathVariable String pnrCode) {
        return ResponseEntity.ok(bookingService.getBookingByPnr(pnrCode));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<List<BookingResponse>> getBookingsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/flight/{flightId}")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF')")
    public ResponseEntity<List<BookingResponse>> getBookingsByFlight(@PathVariable UUID flightId) {
        return ResponseEntity.ok(bookingService.getBookingsByFlight(flightId));
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AIRLINE_STAFF')")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable UUID bookingId,
                                                        @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ResponseEntity.ok(bookingService.updateStatus(bookingId, request));
    }

    @PostMapping("/fare/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<FareSummaryResponse> calculateFare(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.calculateFare(request));
    }

    @PutMapping("/{bookingId}/addons")
    @PreAuthorize("hasAnyRole('ADMIN','PASSENGER')")
    public ResponseEntity<BookingResponse> addAddOn(@PathVariable UUID bookingId,
                                                    @Valid @RequestBody AddOnRequest request) {
        return ResponseEntity.ok(bookingService.addAddOn(bookingId, request));
    }
}
