package com.skybooker.booking.service;

import com.skybooker.booking.dto.*;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

    BookingResponse confirmBooking(UUID bookingId);

    BookingResponse getBookingById(UUID bookingId);

    BookingTicketDetailsResponse getBookingTicketDetails(UUID bookingId);

    BookingResponse getBookingByPnr(String pnrCode);

    List<BookingResponse> getBookingsByUser(UUID userId);

    List<BookingResponse> getBookingsByFlight(UUID flightId);

    BookingResponse cancelBooking(UUID bookingId);

    BookingResponse updateStatus(UUID bookingId, BookingStatusUpdateRequest request);

    FareSummaryResponse calculateFare(BookingRequest request);

    BookingResponse addAddOn(UUID bookingId, AddOnRequest request);

    String generatePnr();

    String generateHoldReference();
}
