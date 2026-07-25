package co.istad.rentiq_api.features.favorite.mapper;

import co.istad.rentiq_api.features.favorite.dto.response.FavoriteResponse;
import co.istad.rentiq_api.features.favorite.entity.Favorite;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.entity.ItemImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "itemId", source = "favorite.item.id")
    @Mapping(target = "title", source = "favorite.item.title")
    @Mapping(target = "thumbnailUrl", expression = "java(resolveThumbnail(favorite.getItem()))")
    @Mapping(target = "pricePerDay", source = "favorite.item.pricePerDay")
    @Mapping(target = "averageRating", source = "favorite.item.averageRating")
    @Mapping(target = "totalReviews", source = "favorite.item.totalReviews")
    @Mapping(target = "locationText", source = "favorite.item.locationText")
    @Mapping(target = "favoritedAt", source = "favorite.createdAt")
    FavoriteResponse toResponse(Favorite favorite);

    // ASSUMPTION (ItemImage entity not yet confirmed): assumes ItemImage has
    // getIsPrimary() -> Boolean and getThumbnailUrl() -> String.
    // Update accessor names here once ItemImage is shared.
    default String resolveThumbnail(Item item) {
        if (item.getImages() == null || item.getImages().isEmpty()) {
            return null;
        }
        return item.getImages().stream()
                .filter(ItemImage::isPrimary)
                .findFirst()
                .orElse(item.getImages().get(0))
                .getThumbnailUrl();
    }
}