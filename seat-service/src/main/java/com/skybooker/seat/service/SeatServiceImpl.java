package com.skybooker.seat.service;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import com.skybooker.seat.exception.InvalidSeatOperationException;
import com.skybooker.seat.exception.ResourceNotFoundException;
import com.skybooker.seat.exception.SeatAlreadyExistsException;
import com.skybooker.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private static final Logger logger = LoggerFactory.getLogger(SeatServiceImpl.class);

    private final SeatRepository seatRepository;

    @Override
    public SeatResponse addSeat(SeatRequest request) {
        validateSeatUniqueness(request.getFlightId(), request.getSeatNumber());

        Seat seat = Seat.builder()
                .flightId(request.getFlightId())
                .seatNumber(request.getSeatNumber().toUpperCase())
                .seatClass(request.getSeatClass())
                .rowNumber(request.getRowNumber())
                .columnLetter(request.getColumnLetter().toUpperCase())
                .isWindow(request.getIsWindow())
                .isAisle(request.getIsAisle())
                .hasExtraLegroom(request.getHasExtraLegroom())
                .status(SeatStatus.AVAILABLE)
                .priceMultiplier(request.getPriceMultiplier())
                .heldAt(null)
                .holdReference(null)
                .build();

        logger.info("Adding seat {} for flight {}", seat.getSeatNumber(), seat.getFlightId());
        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    public List<SeatResponse> addSeatsInBulk(BulkSeatRequest request) {
        List<SeatResponse> createdSeats = new ArrayList<>();
        String columns = request.getSeatColumns().toUpperCase();

        for (int row = request.getFromRow(); row <= request.getToRow(); row++) {
            for (int i = 0; i < columns.length(); i++) {
                String col = String.valueOf(columns.charAt(i));
                String seatNumber = row + col;

                seatRepository.findByFlightIdAndSeatNumber(request.getFlightId(), seatNumber)
                        .ifPresent(existing -> {
                            throw new SeatAlreadyExistsException("Seat already exists: " + seatNumber);
                        });

                boolean isWindow = i == 0 || i == columns.length() - 1;
                boolean isAisle = columns.length() > 3 && (i == 2 || i == 3);

                Seat seat = Seat.builder()
                        .flightId(request.getFlightId())
                        .seatNumber(seatNumber)
                        .seatClass(request.getSeatClass())
                        .rowNumber(row)
                        .columnLetter(col)
                        .isWindow(isWindow)
                        .isAisle(isAisle)
                        .hasExtraLegroom(request.getExtraLegroom())
                        .status(SeatStatus.AVAILABLE)
                        .priceMultiplier(request.getPriceMultiplier())
                        .heldAt(null)
                        .holdReference(null)
                        .build();

                createdSeats.add(mapToResponse(seatRepository.save(seat)));
            }
        }

        logger.info("Bulk created {} seats for flight {}", createdSeats.size(), request.getFlightId());
        return createdSeats;
    }

    @Override
    public List<SeatResponse> getSeatMap(UUID flightId) {
        return seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc(flightId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getAvailableSeats(UUID flightId) {
        return seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getAvailableSeatsByClass(UUID flightId, SeatClass seatClass) {
        return seatRepository.findByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getSeatsByIds(List<UUID> seatIds) {
        return loadSeatsByIds(seatIds)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SeatResponse holdSeat(UUID seatId, String holdReference) {
        return holdSeatsInBulk(List.of(seatId), holdReference).get(0);
    }

    @Override
    public SeatResponse releaseSeat(UUID seatId, String holdReference) {
        return releaseSeatsInBulk(List.of(seatId), holdReference).get(0);
    }

    @Override
    public SeatResponse confirmSeat(UUID seatId, String holdReference) {
        return confirmSeatsInBulk(List.of(seatId), holdReference).get(0);
    }

    @Override
    @Transactional
    public List<SeatResponse> holdSeatsInBulk(List<UUID> seatIds, String holdReference) {
        if (holdReference == null || holdReference.isBlank()) {
            throw new InvalidSeatOperationException("Hold reference is required");
        }

        List<Seat> seats = loadSeatsByIds(seatIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new InvalidSeatOperationException("All seats must be available before hold");
            }
        }

        LocalDateTime heldAt = LocalDateTime.now();
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldAt(heldAt);
            seat.setHoldReference(holdReference);
        }

        logger.info("Held {} seats with reference {}", seats.size(), holdReference);
        return seatRepository.saveAll(seats).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> releaseSeatsInBulk(List<UUID> seatIds, String holdReference) {
        List<Seat> seats = loadSeatsByIds(seatIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD && seat.getStatus() != SeatStatus.CONFIRMED) {
                throw new InvalidSeatOperationException("Only held or confirmed seats can be released");
            }

            if (seat.getStatus() == SeatStatus.HELD
                    && holdReference != null
                    && !holdReference.isBlank()
                    && !holdReference.equals(seat.getHoldReference())) {
                throw new InvalidSeatOperationException("Invalid hold reference for release");
            }
        }

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldAt(null);
            seat.setHoldReference(null);
        }

        logger.info("Released {} seats", seats.size());
        return seatRepository.saveAll(seats).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> confirmSeatsInBulk(List<UUID> seatIds, String holdReference) {
        List<Seat> seats = loadSeatsByIds(seatIds);

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new InvalidSeatOperationException("All seats must be held before confirm");
            }

            if (holdReference != null
                    && !holdReference.isBlank()
                    && !holdReference.equals(seat.getHoldReference())) {
                throw new InvalidSeatOperationException("Invalid hold reference for confirm");
            }
        }

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.CONFIRMED);
            seat.setHeldAt(null);
        }

        logger.info("Confirmed {} seats", seats.size());
        return seatRepository.saveAll(seats).stream().map(this::mapToResponse).toList();
    }

    @Override
    public SeatCountResponse countAvailableByClass(UUID flightId, SeatClass seatClass) {
        long count = seatRepository.countByFlightIdAndSeatClassAndStatus(
                flightId,
                seatClass,
                SeatStatus.AVAILABLE
        );

        return SeatCountResponse.builder()
                .flightId(flightId)
                .seatClass(seatClass)
                .availableCount(count)
                .build();
    }

    @Override
    public int autoReleaseExpiredHolds() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(15);

        List<Seat> expiredSeats = seatRepository.findByStatusAndHeldAtBefore(SeatStatus.HELD, expiryTime);

        for (Seat seat : expiredSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldAt(null);
            seat.setHoldReference(null);
        }

        seatRepository.saveAll(expiredSeats);

        if (!expiredSeats.isEmpty()) {
            logger.info("Auto released {} expired held seats", expiredSeats.size());
        }

        return expiredSeats.size();
    }

    @Override
    public void deleteSeatsForFlight(UUID flightId) {
        seatRepository.deleteByFlightId(flightId);
        logger.info("Deleted all seats for flight {}", flightId);
    }

    private Seat getSeatEntity(UUID seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
    }

    private List<Seat> loadSeatsByIds(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new InvalidSeatOperationException("At least one seat must be provided");
        }

        Set<UUID> uniqueSeatIds = new HashSet<>(seatIds);
        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new InvalidSeatOperationException("Duplicate seat IDs are not allowed");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more seats were not found");
        }

        return seats;
    }

    private void validateSeatUniqueness(UUID flightId, String seatNumber) {
        seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber.toUpperCase())
                .ifPresent(seat -> {
                    throw new SeatAlreadyExistsException("Seat already exists for this flight");
                });
    }

    private SeatResponse mapToResponse(Seat seat) {
        return SeatResponse.builder()
                .seatId(seat.getSeatId())
                .flightId(seat.getFlightId())
                .seatNumber(seat.getSeatNumber())
                .seatClass(seat.getSeatClass())
                .rowNumber(seat.getRowNumber())
                .columnLetter(seat.getColumnLetter())
                .isWindow(seat.getIsWindow())
                .isAisle(seat.getIsAisle())
                .hasExtraLegroom(seat.getHasExtraLegroom())
                .status(seat.getStatus())
                .priceMultiplier(seat.getPriceMultiplier())
                .heldAt(seat.getHeldAt())
                .holdReference(seat.getHoldReference())
                .build();
    }
}
