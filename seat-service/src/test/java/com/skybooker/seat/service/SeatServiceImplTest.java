package com.skybooker.seat.service;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import com.skybooker.seat.exception.InvalidSeatOperationException;
import com.skybooker.seat.exception.ResourceNotFoundException;
import com.skybooker.seat.exception.SeatAlreadyExistsException;
import com.skybooker.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatServiceImpl seatService;

    private UUID seatId;
    private UUID flightId;
    private Seat seat;

    @BeforeEach
    void setUp() {
        seatId = UUID.randomUUID();
        flightId = UUID.randomUUID();

        seat = Seat.builder()
                .seatId(seatId)
                .flightId(flightId)
                .seatNumber("1A")
                .seatClass(SeatClass.ECONOMY)
                .rowNumber(1)
                .columnLetter("A")
                .isWindow(true)
                .isAisle(false)
                .hasExtraLegroom(false)
                .status(SeatStatus.AVAILABLE)
                .priceMultiplier(BigDecimal.valueOf(1.00))
                .heldAt(null)
                .holdReference(null)
                .build();
    }

    @Test
    void addSeat_success() {
        SeatRequest request = new SeatRequest();
        request.setFlightId(flightId);
        request.setSeatNumber("1a");
        request.setSeatClass(SeatClass.ECONOMY);
        request.setRowNumber(1);
        request.setColumnLetter("a");
        request.setIsWindow(true);
        request.setIsAisle(false);
        request.setHasExtraLegroom(false);
        request.setPriceMultiplier(BigDecimal.valueOf(1.00));

        when(seatRepository.findByFlightIdAndSeatNumber(flightId, "1A")).thenReturn(Optional.empty());
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);

        SeatResponse response = seatService.addSeat(request);

        assertEquals("1A", response.getSeatNumber());
        assertEquals(SeatStatus.AVAILABLE, response.getStatus());
        verify(seatRepository).save(any(Seat.class));
    }

    @Test
    void addSeat_whenAlreadyExists_throwException() {
        SeatRequest request = new SeatRequest();
        request.setFlightId(flightId);
        request.setSeatNumber("1A");

        when(seatRepository.findByFlightIdAndSeatNumber(flightId, "1A"))
                .thenReturn(Optional.of(seat));

        assertThrows(SeatAlreadyExistsException.class, () -> seatService.addSeat(request));
        verify(seatRepository, never()).save(any());
    }

    @Test
    void addSeatsInBulk_success() {
        BulkSeatRequest request = new BulkSeatRequest();
        request.setFlightId(flightId);
        request.setFromRow(1);
        request.setToRow(1);
        request.setSeatColumns("ABC");
        request.setSeatClass(SeatClass.ECONOMY);
        request.setExtraLegroom(false);
        request.setPriceMultiplier(BigDecimal.valueOf(1.00));

        when(seatRepository.findByFlightIdAndSeatNumber(eq(flightId), anyString()))
                .thenReturn(Optional.empty());
        when(seatRepository.save(any(Seat.class)))
                .thenAnswer(invocation -> {
                    Seat saved = invocation.getArgument(0);
                    saved.setSeatId(UUID.randomUUID());
                    return saved;
                });

        List<SeatResponse> responses = seatService.addSeatsInBulk(request);

        assertEquals(3, responses.size());
        assertEquals("1A", responses.get(0).getSeatNumber());
        assertEquals("1B", responses.get(1).getSeatNumber());
        assertEquals("1C", responses.get(2).getSeatNumber());
        verify(seatRepository, times(3)).save(any(Seat.class));
    }

    @Test
    void addSeatsInBulk_whenSeatExists_throwException() {
        BulkSeatRequest request = new BulkSeatRequest();
        request.setFlightId(flightId);
        request.setFromRow(1);
        request.setToRow(1);
        request.setSeatColumns("A");
        request.setSeatClass(SeatClass.ECONOMY);
        request.setExtraLegroom(false);
        request.setPriceMultiplier(BigDecimal.valueOf(1.00));

        when(seatRepository.findByFlightIdAndSeatNumber(flightId, "1A"))
                .thenReturn(Optional.of(seat));

        assertThrows(SeatAlreadyExistsException.class, () -> seatService.addSeatsInBulk(request));
        verify(seatRepository, never()).save(any());
    }

    @Test
    void getSeatMap_success() {
        when(seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc(flightId))
                .thenReturn(List.of(seat));

        List<SeatResponse> responses = seatService.getSeatMap(flightId);

        assertEquals(1, responses.size());
        assertEquals("1A", responses.get(0).getSeatNumber());
    }

    @Test
    void getAvailableSeats_success() {
        when(seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE))
                .thenReturn(List.of(seat));

        List<SeatResponse> responses = seatService.getAvailableSeats(flightId);

        assertEquals(1, responses.size());
        assertEquals(SeatStatus.AVAILABLE, responses.get(0).getStatus());
    }

    @Test
    void getAvailableSeatsByClass_success() {
        when(seatRepository.findByFlightIdAndSeatClassAndStatus(
                flightId, SeatClass.ECONOMY, SeatStatus.AVAILABLE))
                .thenReturn(List.of(seat));

        List<SeatResponse> responses = seatService.getAvailableSeatsByClass(flightId, SeatClass.ECONOMY);

        assertEquals(1, responses.size());
        assertEquals(SeatClass.ECONOMY, responses.get(0).getSeatClass());
    }

    @Test
    void holdSeat_success() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = seatService.holdSeat(seatId, "HOLD123");

        assertEquals(SeatStatus.HELD, response.getStatus());
        assertEquals("HOLD123", response.getHoldReference());
        assertNotNull(response.getHeldAt());
    }

    @Test
    void holdSeat_whenSeatNotAvailable_throwException() {
        seat.setStatus(SeatStatus.CONFIRMED);
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThrows(InvalidSeatOperationException.class,
                () -> seatService.holdSeat(seatId, "HOLD123"));
    }

    @Test
    void holdSeat_whenSeatNotFound_throwException() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatService.holdSeat(seatId, "HOLD123"));
    }

    @Test
    void releaseSeat_success() {
        seat.setStatus(SeatStatus.HELD);
        seat.setHoldReference("HOLD123");
        seat.setHeldAt(LocalDateTime.now());

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = seatService.releaseSeat(seatId, "HOLD123");

        assertEquals(SeatStatus.AVAILABLE, response.getStatus());
        assertNull(response.getHoldReference());
        assertNull(response.getHeldAt());
    }

    @Test
    void releaseSeat_whenNotHeld_throwException() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThrows(InvalidSeatOperationException.class,
                () -> seatService.releaseSeat(seatId, "HOLD123"));
    }

    @Test
    void releaseSeat_whenInvalidReference_throwException() {
        seat.setStatus(SeatStatus.HELD);
        seat.setHoldReference("RIGHT");

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThrows(InvalidSeatOperationException.class,
                () -> seatService.releaseSeat(seatId, "WRONG"));
    }

    @Test
    void confirmSeat_success() {
        seat.setStatus(SeatStatus.HELD);
        seat.setHoldReference("HOLD123");
        seat.setHeldAt(LocalDateTime.now());

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = seatService.confirmSeat(seatId, "HOLD123");

        assertEquals(SeatStatus.CONFIRMED, response.getStatus());
        assertEquals("HOLD123", response.getHoldReference());
        assertNull(response.getHeldAt());
    }

    @Test
    void confirmSeat_whenNotHeld_throwException() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThrows(InvalidSeatOperationException.class,
                () -> seatService.confirmSeat(seatId, "HOLD123"));
    }

    @Test
    void confirmSeat_whenInvalidReference_throwException() {
        seat.setStatus(SeatStatus.HELD);
        seat.setHoldReference("RIGHT");

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThrows(InvalidSeatOperationException.class,
                () -> seatService.confirmSeat(seatId, "WRONG"));
    }

    @Test
    void countAvailableByClass_success() {
        when(seatRepository.countByFlightIdAndSeatClassAndStatus(
                flightId, SeatClass.ECONOMY, SeatStatus.AVAILABLE))
                .thenReturn(10L);

        SeatCountResponse response = seatService.countAvailableByClass(flightId, SeatClass.ECONOMY);

        assertEquals(flightId, response.getFlightId());
        assertEquals(SeatClass.ECONOMY, response.getSeatClass());
        assertEquals(10L, response.getAvailableCount());
    }

    @Test
    void autoReleaseExpiredHolds_success() {
        Seat heldSeat = Seat.builder()
                .seatId(UUID.randomUUID())
                .flightId(flightId)
                .seatNumber("2A")
                .seatClass(SeatClass.ECONOMY)
                .rowNumber(2)
                .columnLetter("A")
                .isWindow(true)
                .isAisle(false)
                .hasExtraLegroom(false)
                .status(SeatStatus.HELD)
                .priceMultiplier(BigDecimal.valueOf(1.00))
                .heldAt(LocalDateTime.now().minusMinutes(20))
                .holdReference("OLD-HOLD")
                .build();

        when(seatRepository.findByStatusAndHeldAtBefore(eq(SeatStatus.HELD), any(LocalDateTime.class)))
                .thenReturn(List.of(heldSeat));
        when(seatRepository.saveAll(anyList())).thenReturn(List.of(heldSeat));

        int releasedCount = seatService.autoReleaseExpiredHolds();

        assertEquals(1, releasedCount);
        assertEquals(SeatStatus.AVAILABLE, heldSeat.getStatus());
        assertNull(heldSeat.getHeldAt());
        assertNull(heldSeat.getHoldReference());
    }

    @Test
    void deleteSeatsForFlight_success() {
        seatService.deleteSeatsForFlight(flightId);

        verify(seatRepository).deleteByFlightId(flightId);
    }
}