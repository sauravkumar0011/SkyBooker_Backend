package com.skybooker.passenger.service;

import com.skybooker.passenger.client.BookingClient;
import com.skybooker.passenger.dto.PassengerDetailsRequest;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.dto.PassengerUpdateRequest;
import com.skybooker.passenger.dto.external.BookingSummaryResponse;
import com.skybooker.passenger.entity.Passenger;
import com.skybooker.passenger.exception.ResourceNotFoundException;
import com.skybooker.passenger.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private BookingClient bookingClient;

    @InjectMocks
    private PassengerServiceImpl passengerService;

    private UUID passengerId;
    private UUID bookingId;
    private UUID seatId;
    private Passenger passenger;
    private PassengerRequest request;
    private PassengerUpdateRequest updateRequest;
    private BookingSummaryResponse booking;

    @BeforeEach
    void setUp() {
        passengerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        PassengerDetailsRequest passengerDetails = new PassengerDetailsRequest();
        passengerDetails.setSeatId(seatId);
        passengerDetails.setFirstName("Himanshu");
        passengerDetails.setLastName("Kumar");
        passengerDetails.setDateOfBirth(LocalDate.of(2000, 1, 1));
        passengerDetails.setGender("MALE");
        passengerDetails.setPassportNumber("ABC12345");
        passengerDetails.setNationality("Indian");

        request = new PassengerRequest();
        request.setBookingId(bookingId);
        request.setPassengers(List.of(passengerDetails));

        updateRequest = new PassengerUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Kumar");
        updateRequest.setDateOfBirth(LocalDate.of(2000, 1, 1));
        updateRequest.setGender("MALE");
        updateRequest.setPassportNumber("ABC12345");
        updateRequest.setNationality("Indian");

        booking = new BookingSummaryResponse();
        booking.setBookingId(bookingId);
        booking.setSeatIds(List.of(seatId));
        booking.setTotalPassengers(1);
        booking.setStatus("PENDING");

        passenger = Passenger.builder()
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
    void addPassenger_success() {
        when(bookingClient.getBookingById(bookingId)).thenReturn(booking);
        when(passengerRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(passengerRepository.existsByBookingIdAndSeatId(bookingId, seatId)).thenReturn(false);
        when(passengerRepository.saveAll(anyList())).thenReturn(List.of(passenger));

        List<PassengerResponse> response = passengerService.addPassengers(request);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(bookingId, response.get(0).getBookingId());
        assertEquals("Himanshu", response.get(0).getFirstName());
        assertNotNull(response.get(0).getTicketNumber());

        verify(passengerRepository).saveAll(anyList());
    }

    @Test
    void getByBooking_success() {
        when(passengerRepository.findByBookingId(bookingId)).thenReturn(List.of(passenger));

        List<PassengerResponse> responses = passengerService.getByBooking(bookingId);

        assertEquals(1, responses.size());
        assertEquals(bookingId, responses.get(0).getBookingId());
    }

    @Test
    void getById_success() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));

        PassengerResponse response = passengerService.getById(passengerId);

        assertEquals(passengerId, response.getPassengerId());
        assertEquals("Himanshu", response.getFirstName());
    }

    @Test
    void getById_notFound_throwException() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.getById(passengerId));
    }

    @Test
    void updatePassenger_success() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0));

        PassengerResponse response = passengerService.updatePassenger(passengerId, updateRequest);

        assertEquals("Updated", response.getFirstName());
        verify(passengerRepository).save(passenger);
    }

    @Test
    void updatePassenger_notFound_throwException() {
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passengerService.updatePassenger(passengerId, updateRequest));
    }

    @Test
    void deletePassenger_success() {
        when(passengerRepository.existsById(passengerId)).thenReturn(true);

        passengerService.deletePassenger(passengerId);

        verify(passengerRepository).deleteById(passengerId);
    }
}
