package co.istad.rentiq_api.features.item.mapper;

import co.istad.rentiq_api.features.item.dto.request.CreateItemRequest;
import co.istad.rentiq_api.features.item.dto.request.UpdateItemRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.AdminItemResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.entity.ItemImage;
import org.mapstruct.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = ItemImageMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", source = "authenticatedUserId")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "featuredUntil", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalBookings", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "favoriteCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(
            target = "specifications",
            source = "request.specifications",
            qualifiedByName = "copySpecifications"
    )
    Item toEntity(
            CreateItemRequest request,
            String authenticatedUserId
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "featuredUntil", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalBookings", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "favoriteCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(
            target = "specifications",
            source = "request.specifications",
            qualifiedByName = "copySpecifications"
    )
    void updateEntity(
            UpdateItemRequest request,
            @MappingTarget Item item
    );

    @Mapping(
            target = "primaryImageUrl",
            expression = "java(findPrimaryImageUrl(item))"
    )
    @Mapping(
            target = "specifications",
            source = "specifications",
            qualifiedByName = "copySpecifications"
    )
    ItemResponse toResponse(Item item);

    default AdminItemResponse toAdminResponse(Item item) {
        return new AdminItemResponse(
                toResponse(item),
                item.getApprovedBy(),
                item.getApprovedAt(),
                item.getRejectionReason()
        );
    }

    default String findPrimaryImageUrl(Item item) {
        if (item.getImages() == null || item.getImages().isEmpty()) {
            return null;
        }

        return item.getImages()
                .stream()
                .filter(ItemImage::isPrimary)
                .findFirst()
                .orElse(item.getImages().getFirst())
                .getImageUrl();
    }

    @Named("copySpecifications")
    default Map<String, Object> copySpecifications(
            Map<String, Object> specifications
    ) {
        if (specifications == null) {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>(specifications);
    }
}
