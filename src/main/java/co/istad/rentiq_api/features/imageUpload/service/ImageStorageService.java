package co.istad.rentiq_api.features.imageUpload.service;

import co.istad.rentiq_api.features.imageUpload.dto.StoredImage;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);
}
