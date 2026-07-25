package co.istad.rentiq_api.features.bookingInspection.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        UUID bookingId,
        String checkInNotes,
        String checkOutNotes,
        Instant createdAt,
        List<InspectionImageResponse> images
) {}