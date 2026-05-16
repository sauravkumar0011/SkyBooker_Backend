package com.skybooker.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import com.skybooker.seat.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SeatService seatService;

    private UUID seatId;
    private UUID flightId;
    private SeatResponse response;

    @BeforeEach
    void setUp() {
        seatId = UUID.randomUUID();
        flightId = UUID.randomUUID();

        SeatController seatController = new SeatController(seatService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(seatController)
                .setValidator(null)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        response = SeatResponse.builder()
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
                .priceMultiplier(BigDecimal.valueOf(1.10))
                .heldAt(null)
                .holdReference(null)
                .build();
    }

    @Test
    void addSeat_success() throws Exception {
        SeatRequest request = new SeatRequest();
        request.setFlightId(flightId);
        request.setSeatNumber("1A");
        request.setSeatClass(SeatClass.ECONOMY);
        request.setRowNumber(1);
        request.setColumnLetter("A");
        request.setIsWindow(true);
        request.setIsAisle(false);
        request.setHasExtraLegroom(false);
        request.setPriceMultiplier(BigDecimal.valueOf(1.10));

        when(seatService.addSeat(any(SeatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/seats")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatId").value(seatId.toString()))
                .andExpect(jsonPath("$.seatNumber").value("1A"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void addSeatsInBulk_success() throws Exception {
        BulkSeatRequest request = new BulkSeatRequest();
        request.setFlightId(flightId);
        request.setFromRow(1);
        request.setToRow(1);
        request.setSeatColumns("ABC");
        request.setSeatClass(SeatClass.ECONOMY);
        request.setExtraLegroom(false);
        request.setPriceMultiplier(BigDecimal.valueOf(1.10));

        when(seatService.addSeatsInBulk(any(BulkSeatRequest.class))).thenReturn(List.of(response));

        mockMvc.perform(post("/seats/bulk")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].seatNumber").value("1A"));
    }

    @Test
    void getSeatMap_success() throws Exception {
        when(seatService.getSeatMap(flightId)).thenReturn(List.of(response));

        mockMvc.perform(get("/seats/flight/{flightId}/map", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatNumber").value("1A"));
    }

    @Test
    void getAvailableSeats_success() throws Exception {
        when(seatService.getAvailableSeats(flightId)).thenReturn(List.of(response));

        mockMvc.perform(get("/seats/flight/{flightId}/available", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void getAvailableSeatsByClass_success() throws Exception {
        when(seatService.getAvailableSeatsByClass(flightId, SeatClass.ECONOMY))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/seats/flight/{flightId}/available/{seatClass}", flightId, SeatClass.ECONOMY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatClass").value("ECONOMY"));
    }

    @Test
    void holdSeat_success() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest();
        request.setHoldReference("HOLD123");

        SeatResponse heldResponse = SeatResponse.builder()
                .seatId(seatId)
                .flightId(flightId)
                .seatNumber("1A")
                .seatClass(SeatClass.ECONOMY)
                .rowNumber(1)
                .columnLetter("A")
                .isWindow(true)
                .isAisle(false)
                .hasExtraLegroom(false)
                .status(SeatStatus.HELD)
                .priceMultiplier(BigDecimal.valueOf(1.00))
                .holdReference("HOLD123")
                .build();

        when(seatService.holdSeat(seatId, "HOLD123")).thenReturn(heldResponse);

        mockMvc.perform(put("/seats/{seatId}/hold", seatId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.holdReference").value("HOLD123"));
    }

    @Test
    void releaseSeat_success() throws Exception {
        ReleaseSeatRequest request = new ReleaseSeatRequest();
        request.setHoldReference("HOLD123");

        when(seatService.releaseSeat(seatId, "HOLD123")).thenReturn(response);

        mockMvc.perform(put("/seats/{seatId}/release", seatId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void confirmSeat_success() throws Exception {
        ConfirmSeatRequest request = new ConfirmSeatRequest();
        request.setHoldReference("HOLD123");

        SeatResponse confirmedResponse = SeatResponse.builder()
                .seatId(seatId)
                .flightId(flightId)
                .seatNumber("1A")
                .seatClass(SeatClass.ECONOMY)
                .rowNumber(1)
                .columnLetter("A")
                .isWindow(true)
                .isAisle(false)
                .hasExtraLegroom(false)
                .status(SeatStatus.CONFIRMED)
                .priceMultiplier(BigDecimal.valueOf(1.00))
                .holdReference("HOLD123")
                .build();

        when(seatService.confirmSeat(seatId, "HOLD123")).thenReturn(confirmedResponse);

        mockMvc.perform(put("/seats/{seatId}/confirm", seatId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void countAvailableByClass_success() throws Exception {
        SeatCountResponse countResponse = SeatCountResponse.builder()
                .flightId(flightId)
                .seatClass(SeatClass.ECONOMY)
                .availableCount(10L)
                .build();

        when(seatService.countAvailableByClass(flightId, SeatClass.ECONOMY))
                .thenReturn(countResponse);

        mockMvc.perform(get("/seats/flight/{flightId}/count/{seatClass}", flightId, SeatClass.ECONOMY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCount").value(10));
    }

    @Test
    void deleteSeatsForFlight_success() throws Exception {
        mockMvc.perform(delete("/seats/flight/{flightId}", flightId))
                .andExpect(status().isOk())
                .andExpect(content().string("Seats deleted successfully for flight"));

        verify(seatService).deleteSeatsForFlight(flightId);
    }
}