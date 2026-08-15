package co.istad.rentiq_api.features.imageUpload.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageUploadResponse(
        UUID id,
        String imageUrl,
        String thumbnailUrl,
        String publicId,
        String assetId,
        String folder,
        String originalFilename,
        String contentType,
        Long fileSize,
        OffsetDateTime createdAt
) {
}
