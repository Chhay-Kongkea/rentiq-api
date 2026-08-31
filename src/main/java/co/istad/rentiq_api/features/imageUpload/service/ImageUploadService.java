package co.istad.rentiq_api.features.imageUpload.service;

import co.istad.rentiq_api.features.imageUpload.dto.response.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code getById}/{@code delete} were removed (backend audit SEC-004): the underlying
 * {@code UploadedImage} record has no owner field, so those endpoints let any authenticated
 * user view or delete any other user's uploaded image. Feature-specific image flows (item
 * images, avatars, KYC, review images) each have their own ownership-scoped upload/delete path
 * and never used this generic service — only {@code upload} remains.
 */
public interface ImageUploadService {

    ImageUploadResponse upload(MultipartFile file, String folder);
}
