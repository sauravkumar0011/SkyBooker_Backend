package com.skybooker.passenger.service;

import java.util.List;
import java.util.UUID;

import com.skybooker.passenger.dto.PassengerUpdateRequest;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;

public interface PassengerService {

    List<PassengerResponse> addPassengers(PassengerRequest request);

    List<PassengerResponse> getByBooking(UUID bookingId);

    PassengerResponse getById(UUID passengerId);

    PassengerResponse updatePassenger(UUID passengerId, PassengerUpdateRequest request);

    void deletePassenger(UUID passengerId);

}
