package com.skybooker.booking.dto;

import com.skybooker.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingStatusUpdateRequest {
    @NotNull
    private BookingStatus status;
}