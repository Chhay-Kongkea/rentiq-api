package co.istad.rentiq_api.features.item.dto.request;

import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import jakarta.validation.constraints.Size;

public record AdminItemFilter(
        @Size(max = 200, message = "Keyword cannot exceed 200 characters")
        String keyword,
        ItemApprovalStatus approvalStatus
) {
}
