package co.istad.rentiq_api.features.item.dto.storage;


public record StoredImage(
        String imageUrl,
        String thumbnailUrl,
        String publicId,
        String assetId
) {
}
