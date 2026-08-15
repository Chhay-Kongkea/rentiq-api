package co.istad.rentiq_api.features.bookings.dto.request;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingStatusRequest(

        @NotNull
        BookingStatus status,

        String reason

) {}
