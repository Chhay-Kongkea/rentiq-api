package co.istad.rentiq_api.features.imageUpload.service.impl;

import co.istad.rentiq_api.features.imageUpload.dto.StoredImage;
import co.istad.rentiq_api.features.imageUpload.exception.ImageStorageException;
import co.istad.rentiq_api.features.imageUpload.exception.InvalidImageException;
import co.istad.rentiq_api.features.imageUpload.service.ImageStorageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;

    @Override
    public StoredImage uploadImage(
            MultipartFile file,
            String folder
    ) {

        validateImage(file);

        try {

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );

            String imageUrl =
                    String.valueOf(result.get("secure_url"));

            String publicId =
                    String.valueOf(result.get("public_id"));

            String assetId =
                    result.get("asset_id") != null
                            ? String.valueOf(result.get("asset_id"))
                            : null;

            String thumbnailUrl = cloudinary.url()
                    .secure(true)
                    .transformation(
                            new com.cloudinary.Transformation<>()
                                    .width(400)
                                    .height(400)
                                    .crop("fill")
                                    .quality("auto")
                    )
                    .generate(publicId);

            return new StoredImage(
                    imageUrl,
                    thumbnailUrl,
                    publicId,
                    assetId
            );

        } catch (IOException e) {

            throw new ImageStorageException(
                    "Failed to upload image to Cloudinary",
                    e
            );
        }
    }

    @Override
    public void deleteImage(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            throw new InvalidImageException(
                    "Image publicId is required"
            );
        }

        try {

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

        } catch (IOException e) {

            throw new ImageStorageException(
                    "Failed to delete image from Cloudinary",
                    e
            );
        }
    }

    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    "Image file is required"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new InvalidImageException(
                    "Only image files are allowed"
            );
        }

        long maxSize = 10 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new InvalidImageException(
                    "Image size must not exceed 10MB"
            );
        }
    }
}