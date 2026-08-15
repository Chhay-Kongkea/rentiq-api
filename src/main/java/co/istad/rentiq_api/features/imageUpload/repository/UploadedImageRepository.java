package co.istad.rentiq_api.features.imageUpload.repository;

import co.istad.rentiq_api.features.imageUpload.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, UUID> {
}
