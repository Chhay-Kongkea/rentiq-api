package co.istad.rentiq_api.features.review.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        String reviewerId,
        UUID itemId,
        Short rating,
        String reviewText,
        String vendorReply,
        Instant vendorRepliedAt,
        String status,
        Instant createdAt,
        List<ReviewImageResponse> images
) {}