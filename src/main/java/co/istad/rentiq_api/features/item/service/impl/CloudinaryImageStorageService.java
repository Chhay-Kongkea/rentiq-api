package co.istad.rentiq_api.features.item.service.impl;
import co.istad.rentiq_api.features.item.exception.ImageStorageException;
import co.istad.rentiq_api.features.item.exception.InvalidImageException;
import co.istad.rentiq_api.features.item.dto.storage.StoredImage;
import co.istad.rentiq_api.features.item.service.ImageStorageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final Cloudinary cloudinary;

    @Override
    public StoredImage uploadItemImage(
            MultipartFile file,
            UUID itemId
    ) {
        validateImage(file);

        try {
            Map<?, ?> uploadResult =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder",
                                    "rentiq/items/" + itemId,
                                    "resource_type",
                                    "image",
                                    "unique_filename",
                                    true,
                                    "overwrite",
                                    false
                            )
                    );

            String imageUrl = getRequiredValue(uploadResult, "secure_url");
            String publicId = getRequiredValue(uploadResult, "public_id");
            String assetId = getOptionalValue(uploadResult, "asset_id");

            String thumbnailUrl = buildThumbnailUrl(publicId);

            return new StoredImage(imageUrl, thumbnailUrl, publicId, assetId);

        } catch (IOException exception) {
            throw new ImageStorageException("Failed to read or upload the image", exception);
        } catch (ImageStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ImageStorageException("Cloudinary image upload failed", exception);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new ImageStorageException(
                    "Cloudinary public ID is required"
            );
        }

        try {
            Map<?, ?> deletionResult =
                    cloudinary.uploader().destroy(
                            publicId,
                            ObjectUtils.asMap(
                                    "resource_type",
                                    "image",
                                    "invalidate",
                                    true
                            )
                    );

            String result = getOptionalValue(deletionResult, "result");

            if (!"ok".equalsIgnoreCase(result) && !"not found".equalsIgnoreCase(result)) {
                throw new ImageStorageException(
                        "Cloudinary could not delete image. Result: " + result);
            }

        } catch (IOException exception) {
            throw new ImageStorageException(
                    "Failed to delete image from Cloudinary",
                    exception
            );
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    "Image file is required"
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidImageException(
                    "Image size cannot exceed 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT)
        )) {
            throw new InvalidImageException(
                    "Only JPEG, PNG and WebP images are allowed"
            );
        }
    }

    private String buildThumbnailUrl(String publicId) {
        return cloudinary
                .url()
                .secure(true)
                .resourceType("image")
                .transformation(
                        new Transformation<>()
                                .width(400)
                                .height(300)
                                .crop("fill")
                                .gravity("auto")
                                .quality("auto")
                                .fetchFormat("auto")
                )
                .generate(publicId);
    }

    private String getRequiredValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        if (value == null) {
            throw new ImageStorageException(
                    "Cloudinary response is missing: " + key
            );
        }

        return value.toString();
    }

    private String getOptionalValue(Map<?, ?> result, String key) {
        Object value = result.get(key);

        return value == null
                ? null
                : value.toString();
    }
}
