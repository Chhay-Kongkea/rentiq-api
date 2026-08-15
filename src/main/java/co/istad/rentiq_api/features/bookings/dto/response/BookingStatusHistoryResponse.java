package co.istad.rentiq_api.features.bookings.dto.response;

import co.istad.rentiq_api.features.bookings.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingStatusHistoryResponse(

        UUID id,

        BookingStatus oldStatus,

        BookingStatus newStatus,

        String changedBy,

        String reason,

        OffsetDateTime createdAt

) {}
