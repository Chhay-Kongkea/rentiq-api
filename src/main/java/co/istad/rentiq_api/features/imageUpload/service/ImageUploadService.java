package co.istad.rentiq_api.features.imageUpload.service;

import co.istad.rentiq_api.features.imageUpload.dto.response.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageUploadService {

    ImageUploadResponse upload(MultipartFile file, String folder);

    ImageUploadResponse getById(UUID id);

    void delete(UUID id);
}
