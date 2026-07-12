package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.features.item.dto.request.UpdateItemImageRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ItemImageService {
    List<ItemImageResponse> uploadImages(UUID itemId, List<MultipartFile> files, String authenticatedUserId);
    List<ItemImageResponse> getImages(UUID itemId);
    ItemImageResponse updateImage(UUID itemId, UUID imageId, UpdateItemImageRequest request, String authenticatedUserId);
    void deleteImage(UUID itemId, UUID imageId, String authenticatedUserId);
}