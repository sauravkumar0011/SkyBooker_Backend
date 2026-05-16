package com.skybooker.passenger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skybooker.passenger.dto.PassengerDetailsRequest;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.PassengerUpdateRequest;
import com.skybooker.passenger.service.PassengerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PassengerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PassengerService passengerService;

    private UUID passengerId;
    private UUID bookingId;
    private UUID seatId;
    private PassengerRequest request;
    private PassengerUpdateRequest updateRequest;
    private PassengerResponse response;

    @BeforeEach
    void setUp() {
        passengerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        PassengerController controller = new PassengerController(passengerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PassengerDetailsRequest passenger = new PassengerDetailsRequest();
        passenger.setSeatId(seatId);
        passenger.setFirstName("Himanshu");
        passenger.setLastName("Kumar");
        passenger.setDateOfBirth(LocalDate.of(2000, 1, 1));
        passenger.setGender("MALE");
        passenger.setPassportNumber("ABC12345");
        passenger.setNationality("Indian");

        request = new PassengerRequest();
        request.setBookingId(bookingId);
        request.setPassengers(List.of(passenger));

        updateRequest = new PassengerUpdateRequest();
        updateRequest.setFirstName("Himanshu");
        updateRequest.setLastName("Kumar");
        updateRequest.setDateOfBirth(LocalDate.of(2000, 1, 1));
        updateRequest.setGender("MALE");
        updateRequest.setPassportNumber("ABC12345");
        updateRequest.setNationality("Indian");

        response = PassengerResponse.builder()
                .passengerId(passengerId)
                .bookingId(bookingId)
                .seatId(seatId)
                .firstName("Himanshu")
                .lastName("Kumar")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender("MALE")
                .passportNumber("ABC12345")
                .nationality("Indian")
                .ticketNumber("TKT-12345678")
                .build();
    }

    @Test
    void addPassenger_success() throws Exception {
        when(passengerService.addPassengers(any(PassengerRequest.class))).thenReturn(List.of(response));

        mockMvc.perform(post("/passengers/bulk")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].passengerId").value(passengerId.toString()))
                .andExpect(jsonPath("$[0].firstName").value("Himanshu"));
    }

    @Test
    void getByBooking_success() throws Exception {
        when(passengerService.getByBooking(bookingId)).thenReturn(List.of(response));

        mockMvc.perform(get("/passengers/booking/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(bookingId.toString()));
    }

    @Test
    void getById_success() throws Exception {
        when(passengerService.getById(passengerId)).thenReturn(response);

        mockMvc.perform(get("/passengers/{id}", passengerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(passengerId.toString()));
    }

    @Test
    void updatePassenger_success() throws Exception {
        when(passengerService.updatePassenger(eq(passengerId), any(PassengerUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/passengers/{id}", passengerId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Himanshu"));
    }

    @Test
    void deletePassenger_success() throws Exception {
        mockMvc.perform(delete("/passengers/{id}", passengerId))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));

        verify(passengerService).deletePassenger(passengerId);
    }
}
