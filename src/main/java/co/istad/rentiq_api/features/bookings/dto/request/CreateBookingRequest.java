package co.istad.rentiq_api.features.bookings.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(

        UUID itemId,

        UUID offerId,

        @NotNull
        @Future
        LocalDate rentalStart,

        @NotNull
        @Future
        LocalDate rentalEnd

) {}
