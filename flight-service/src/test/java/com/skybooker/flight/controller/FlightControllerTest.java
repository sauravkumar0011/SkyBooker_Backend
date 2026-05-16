package com.skybooker.flight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.dto.UpdateFlightStatusRequest;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FlightService flightService;

    private UUID flightId;
    private UUID airlineId;
    private FlightRequest request;
    private FlightResponse response;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        airlineId = UUID.randomUUID();

        FlightController flightController = new FlightController(flightService);
        mockMvc = MockMvcBuilders.standaloneSetup(flightController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new FlightRequest();
        request.setFlightNumber("AI101");
        request.setAirlineId(airlineId);
        request.setOriginAirportCode("DEL");
        request.setDestinationAirportCode("BOM");
        request.setDepartureTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        request.setArrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0));
        request.setAircraftType("Airbus A320");
        request.setTotalSeats(180);
        request.setBasePrice(BigDecimal.valueOf(5500));

        response = FlightResponse.builder()
                .flightId(flightId)
                .flightNumber("AI101")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .durationMinutes(120)
                .status(FlightStatus.ON_TIME)
                .aircraftType("Airbus A320")
                .totalSeats(180)
                .availableSeats(180)
                .basePrice(BigDecimal.valueOf(5500))
                .build();
    }

    @Test
    void addFlight_success() throws Exception {
        when(flightService.addFlight(any(FlightRequest.class))).thenReturn(response);

        mockMvc.perform(post("/flights")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.flightNumber").value("AI101"))
                .andExpect(jsonPath("$.originAirportCode").value("DEL"))
                .andExpect(jsonPath("$.destinationAirportCode").value("BOM"))
                .andExpect(jsonPath("$.status").value("ON_TIME"));
    }

    @Test
    void getAllFlights_success() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(response));

        mockMvc.perform(get("/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flightId").value(flightId.toString()))
                .andExpect(jsonPath("$[0].flightNumber").value("AI101"));
    }

    @Test
    void getFlightById_success() throws Exception {
        when(flightService.getFlightById(flightId)).thenReturn(response);

        mockMvc.perform(get("/flights/{flightId}", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.flightNumber").value("AI101"));
    }

    @Test
    void getFlightByNumber_success() throws Exception {
        when(flightService.getFlightByNumber("AI101")).thenReturn(response);

        mockMvc.perform(get("/flights/number/{flightNumber}", "AI101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("AI101"));
    }

    @Test
    void getFlightsByAirline_success() throws Exception {
        when(flightService.getFlightsByAirline(airlineId)).thenReturn(List.of(response));

        mockMvc.perform(get("/flights/airline/{airlineId}", airlineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].airlineId").value(airlineId.toString()))
                .andExpect(jsonPath("$[0].flightNumber").value("AI101"));
    }

    @Test
    void searchFlights_success() throws Exception {
        LocalDate departureDate = LocalDate.of(2026, 5, 10);

        when(flightService.searchFlights("DEL", "BOM", departureDate))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originAirportCode").value("DEL"))
                .andExpect(jsonPath("$[0].destinationAirportCode").value("BOM"));
    }

    @Test
    void updateFlight_success() throws Exception {
        when(flightService.updateFlight(eq(flightId), any(FlightRequest.class))).thenReturn(response);

        mockMvc.perform(put("/flights/{flightId}", flightId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.flightNumber").value("AI101"));
    }

    @Test
    void updateStatus_success() throws Exception {
        UpdateFlightStatusRequest statusRequest = new UpdateFlightStatusRequest();
        statusRequest.setStatus(FlightStatus.DELAYED);

        FlightResponse delayedResponse = FlightResponse.builder()
                .flightId(flightId)
                .flightNumber("AI101")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .durationMinutes(120)
                .status(FlightStatus.DELAYED)
                .aircraftType("Airbus A320")
                .totalSeats(180)
                .availableSeats(180)
                .basePrice(BigDecimal.valueOf(5500))
                .build();

        when(flightService.updateStatus(flightId, FlightStatus.DELAYED)).thenReturn(delayedResponse);

        mockMvc.perform(put("/flights/{flightId}/status", flightId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELAYED"));
    }

    @Test
    void decrementSeats_success() throws Exception {
        FlightResponse seatResponse = FlightResponse.builder()
                .flightId(flightId)
                .flightNumber("AI101")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 5, 10, 12, 0))
                .durationMinutes(120)
                .status(FlightStatus.ON_TIME)
                .aircraftType("Airbus A320")
                .totalSeats(180)
                .availableSeats(179)
                .basePrice(BigDecimal.valueOf(5500))
                .build();

        when(flightService.decrementSeats(flightId, 1)).thenReturn(seatResponse);

        mockMvc.perform(put("/flights/{flightId}/decrement-seats", flightId)
                        .param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(179));
    }

    @Test
    void incrementSeats_success() throws Exception {
        when(flightService.incrementSeats(flightId, 1)).thenReturn(response);

        mockMvc.perform(put("/flights/{flightId}/increment-seats", flightId)
                        .param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(180));
    }

    @Test
    void deleteFlight_success() throws Exception {
        mockMvc.perform(delete("/flights/{flightId}", flightId))
                .andExpect(status().isOk())
                .andExpect(content().string("Flight deleted successfully"));

        verify(flightService).deleteFlight(flightId);
    }
}