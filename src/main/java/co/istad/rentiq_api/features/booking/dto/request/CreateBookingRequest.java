package co.istad.rentiq_api.features.booking.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID itemId,
        UUID offerId,
        @NotNull @FutureOrPresent LocalDate rentalStart,
        @NotNull @FutureOrPresent LocalDate rentalEnd
) {}