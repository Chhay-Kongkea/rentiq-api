package co.istad.rentiq_api.features.item.dto.respone;

import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemCondition;
import co.istad.rentiq_api.features.item.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String ownerId,
        java.util.UUID categoryId,
        String title,
        String description,
        ItemCondition condition,
        Map<String, Object> specifications,

        List<ItemImageResponse> images,
        String primaryImageUrl,

        String locationText,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal pricePerDay,
        BigDecimal depositAmount,
        boolean available,
        ItemApprovalStatus approvalStatus,
        ItemStatus status,
        boolean featured,
        OffsetDateTime featuredUntil,
        BigDecimal averageRating,
        Integer totalReviews,
        Integer totalBookings,
        Integer viewCount,
        Integer favoriteCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
