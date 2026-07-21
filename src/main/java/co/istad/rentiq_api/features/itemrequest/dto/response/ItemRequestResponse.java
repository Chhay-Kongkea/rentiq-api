package co.istad.rentiq_api.features.itemrequest.dto.response;

import co.istad.rentiq_api.features.itemrequest.enums.ItemRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemRequestResponse(
        UUID id,
        String customerId,
        Short categoryId,
        String title,
        String description,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        LocalDate neededFrom,
        LocalDate neededTo,
        Double latitude,
        Double longitude,
        Short radiusKm,
        ItemRequestStatus status,
        OffsetDateTime expiresAt,
        Integer offerCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}