package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.features.item.dto.storage.StoredImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageStorageService {
    StoredImage uploadItemImage(MultipartFile file, UUID itemId);
    void deleteImage(String publicId);
}
