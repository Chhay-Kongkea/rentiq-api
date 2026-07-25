package co.istad.rentiq_api.features.bookingInspection.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InspectionImageResponse(
        UUID id,
        String imageName,
        String type,
        Instant createdAt
) {}