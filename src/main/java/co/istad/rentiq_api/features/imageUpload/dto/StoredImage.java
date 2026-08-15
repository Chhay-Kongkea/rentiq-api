package co.istad.rentiq_api.features.imageUpload.dto;

public record StoredImage(
        String imageUrl,
        String thumbnailUrl,
        String publicId,
        String assetId
) {
}
