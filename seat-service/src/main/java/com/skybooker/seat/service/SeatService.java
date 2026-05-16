package com.skybooker.seat.service;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.SeatClass;

import java.util.List;
import java.util.UUID;

public interface SeatService {

    SeatResponse addSeat(SeatRequest request);

    List<SeatResponse> addSeatsInBulk(BulkSeatRequest request);

    List<SeatResponse> getSeatMap(UUID flightId);

    List<SeatResponse> getAvailableSeats(UUID flightId);

    List<SeatResponse> getAvailableSeatsByClass(UUID flightId, SeatClass seatClass);

    List<SeatResponse> getSeatsByIds(List<UUID> seatIds);

    SeatResponse holdSeat(UUID seatId, String holdReference);

    SeatResponse releaseSeat(UUID seatId, String holdReference);

    SeatResponse confirmSeat(UUID seatId, String holdReference);

    List<SeatResponse> holdSeatsInBulk(List<UUID> seatIds, String holdReference);

    List<SeatResponse> releaseSeatsInBulk(List<UUID> seatIds, String holdReference);

    List<SeatResponse> confirmSeatsInBulk(List<UUID> seatIds, String holdReference);

    SeatCountResponse countAvailableByClass(UUID flightId, SeatClass seatClass);

    int autoReleaseExpiredHolds();

    void deleteSeatsForFlight(UUID flightId);
}
