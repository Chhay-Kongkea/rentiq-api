package co.istad.rentiq_api.features.imageUpload.service.impl;

import co.istad.rentiq_api.features.imageUpload.dto.StoredImage;
import co.istad.rentiq_api.features.imageUpload.dto.response.ImageUploadResponse;
import co.istad.rentiq_api.features.imageUpload.entity.UploadedImage;
import co.istad.rentiq_api.features.imageUpload.repository.UploadedImageRepository;
import co.istad.rentiq_api.features.imageUpload.service.ImageStorageService;
import co.istad.rentiq_api.features.imageUpload.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private final ImageStorageService imageStorageService;
    private final UploadedImageRepository uploadedImageRepository;

    @Override
    @Transactional
    public ImageUploadResponse upload(MultipartFile file, String folder) {

        String targetFolder =
                folder == null || folder.isBlank()
                        ? "rentiq/general"
                        : "rentiq/" + sanitizeFolder(folder);

        StoredImage storedImage =
                imageStorageService.uploadImage(file, targetFolder);

        UploadedImage image = UploadedImage.builder()
                .imageUrl(storedImage.imageUrl())
                .thumbnailUrl(storedImage.thumbnailUrl())
                .publicId(storedImage.publicId())
                .assetId(storedImage.assetId())
                .folder(targetFolder)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        UploadedImage saved = uploadedImageRepository.save(image);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageUploadResponse getById(UUID id) {

        UploadedImage image = uploadedImageRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Image not found with id: " + id)
                );

        return toResponse(image);
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        UploadedImage image = uploadedImageRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Image not found with id: " + id)
                );

        imageStorageService.deleteImage(image.getPublicId());

        uploadedImageRepository.delete(image);
    }

    private ImageUploadResponse toResponse(UploadedImage image) {

        return new ImageUploadResponse(
                image.getId(),
                image.getImageUrl(),
                image.getThumbnailUrl(),
                image.getPublicId(),
                image.getAssetId(),
                image.getFolder(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getFileSize(),
                image.getCreatedAt()
        );
    }

    private String sanitizeFolder(String folder) {

        String clean = folder
                .trim()
                .toLowerCase()
                .replace("\\", "/")
                .replaceAll("[^a-z0-9/_-]", "");

        clean = clean.replaceAll("/+", "/");

        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }

        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        return clean.isBlank() ? "general" : clean;
    }
}
