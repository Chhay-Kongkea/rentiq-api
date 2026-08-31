package co.istad.rentiq_api.features.item.dto.respone;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.time.OffsetDateTime;

public record AdminItemResponse(
        @JsonUnwrapped
        ItemResponse item,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectionReason
) {
}
