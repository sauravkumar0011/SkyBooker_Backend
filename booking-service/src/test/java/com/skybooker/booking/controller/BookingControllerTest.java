package com.skybooker.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.dto.BookingStatusUpdateRequest;
import com.skybooker.booking.dto.FareSummaryResponse;
import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.entity.TripType;
import com.skybooker.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    private UUID bookingId;
    private UUID userId;
    private UUID flightId;
    private List<UUID> seatIds;
    private BookingRequest request;
    private BookingResponse response;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        seatIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        BookingController bookingController = new BookingController(bookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new BookingRequest();
        request.setUserId(userId);
        request.setFlightId(flightId);
        request.setSeatIds(seatIds);
        request.setTripType(TripType.ONE_WAY);
        request.setMealPreference("VEG");
        request.setLuggageKg(10);
        request.setContactEmail("himanshu@example.com");
        request.setContactPhone("9876543210");

        response = buildResponse(BookingStatus.PENDING, "VEG", 10, BigDecimal.valueOf(15480));
    }

    @Test
    void createBooking_success() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.pnrCode").value("ABC123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.seatIds[0]").value(seatIds.get(0).toString()))
                .andExpect(jsonPath("$.totalPassengers").value(2));
    }

    @Test
    void confirmBooking_success() throws Exception {
        when(bookingService.confirmBooking(bookingId))
                .thenReturn(buildResponse(BookingStatus.CONFIRMED, "VEG", 10, BigDecimal.valueOf(15480)));

        mockMvc.perform(put("/bookings/{bookingId}/confirm", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPassengers").value(2));
    }

    @Test
    void getBookingById_success() throws Exception {
        when(bookingService.getBookingById(bookingId)).thenReturn(response);

        mockMvc.perform(get("/bookings/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.pnrCode").value("ABC123"));
    }

    @Test
    void getBookingByPnr_success() throws Exception {
        when(bookingService.getBookingByPnr("ABC123")).thenReturn(response);

        mockMvc.perform(get("/bookings/pnr/{pnrCode}", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnrCode").value("ABC123"));
    }

    @Test
    void getBookingsByUser_success() throws Exception {
        when(bookingService.getBookingsByUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/bookings/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].pnrCode").value("ABC123"));
    }

    @Test
    void getBookingsByFlight_success() throws Exception {
        when(bookingService.getBookingsByFlight(flightId)).thenReturn(List.of(response));

        mockMvc.perform(get("/bookings/flight/{flightId}", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flightId").value(flightId.toString()))
                .andExpect(jsonPath("$[0].pnrCode").value("ABC123"));
    }

    @Test
    void cancelBooking_success() throws Exception {
        when(bookingService.cancelBooking(bookingId))
                .thenReturn(buildResponse(BookingStatus.CANCELLED, "VEG", 10, BigDecimal.valueOf(15480)));

        mockMvc.perform(put("/bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void updateStatus_success() throws Exception {
        BookingStatusUpdateRequest statusRequest = new BookingStatusUpdateRequest();
        statusRequest.setStatus(BookingStatus.COMPLETED);

        when(bookingService.updateStatus(eq(bookingId), any(BookingStatusUpdateRequest.class)))
                .thenReturn(buildResponse(BookingStatus.COMPLETED, "VEG", 10, BigDecimal.valueOf(15480)));

        mockMvc.perform(put("/bookings/{bookingId}/status", bookingId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void calculateFare_success() throws Exception {
        FareSummaryResponse fareSummary = FareSummaryResponse.builder()
                .seatIds(seatIds)
                .baseFare(BigDecimal.valueOf(11000))
                .taxes(BigDecimal.valueOf(1980))
                .baggageCharge(BigDecimal.valueOf(2000))
                .mealCharge(BigDecimal.valueOf(500))
                .totalFare(BigDecimal.valueOf(15480))
                .totalPassengers(2)
                .build();

        when(bookingService.calculateFare(any(BookingRequest.class))).thenReturn(fareSummary);

        mockMvc.perform(post("/bookings/fare/calculate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFare").value(15480))
                .andExpect(jsonPath("$.baggageCharge").value(2000))
                .andExpect(jsonPath("$.mealCharge").value(500))
                .andExpect(jsonPath("$.totalPassengers").value(2));
    }

    @Test
    void addAddOn_success() throws Exception {
        AddOnRequest addOnRequest = new AddOnRequest();
        addOnRequest.setMealPreference("NON_VEG");
        addOnRequest.setLuggageKg(5);

        when(bookingService.addAddOn(eq(bookingId), any(AddOnRequest.class)))
                .thenReturn(buildResponse(BookingStatus.PENDING, "NON_VEG", 5, BigDecimal.valueOf(14480)));

        mockMvc.perform(put("/bookings/{bookingId}/addons", bookingId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealPreference").value("NON_VEG"))
                .andExpect(jsonPath("$.luggageKg").value(5))
                .andExpect(jsonPath("$.totalFare").value(14480));
    }

    private BookingResponse buildResponse(BookingStatus status,
                                          String mealPreference,
                                          Integer luggageKg,
                                          BigDecimal totalFare) {
        return BookingResponse.builder()
                .bookingId(bookingId)
                .userId(userId)
                .flightId(flightId)
                .seatIds(seatIds)
                .pnrCode("ABC123")
                .tripType(TripType.ONE_WAY)
                .status(status)
                .baseFare(BigDecimal.valueOf(11000))
                .taxes(BigDecimal.valueOf(1980))
                .totalFare(totalFare)
                .totalPassengers(seatIds.size())
                .mealPreference(mealPreference)
                .luggageKg(luggageKg)
                .contactEmail("himanshu@example.com")
                .contactPhone("9876543210")
                .bookedAt(LocalDateTime.now())
                .paymentId(null)
                .build();
    }
}
