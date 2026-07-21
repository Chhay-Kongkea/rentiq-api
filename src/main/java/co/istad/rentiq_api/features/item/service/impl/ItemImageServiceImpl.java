package co.istad.rentiq_api.features.item.service.impl;

import co.istad.rentiq_api.features.item.exception.InvalidImageException;
import co.istad.rentiq_api.features.item.exception.ItemAccessDeniedException;
import co.istad.rentiq_api.features.item.exception.ItemImageNotFoundException;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.dto.request.UpdateItemImageRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemImageResponse;
import co.istad.rentiq_api.features.item.dto.storage.StoredImage;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.entity.ItemImage;
import co.istad.rentiq_api.features.item.mapper.ItemImageMapper;
import co.istad.rentiq_api.features.item.repository.ItemImageRepository;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.item.service.ImageStorageService;
import co.istad.rentiq_api.features.item.service.ItemImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemImageServiceImpl implements ItemImageService {

    private static final int MAX_IMAGES_PER_ITEM = 8;

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final ImageStorageService imageStorageService;
    private final ItemImageMapper itemImageMapper;

    @Override
    @Transactional
    public List<ItemImageResponse> uploadImages(UUID itemId, List<MultipartFile> files, String authenticatedUserId) {
        Item item = getOwnedItem(itemId ,authenticatedUserId);
        validateFiles(files);

        long currentImageCount = itemImageRepository.countByItemId(itemId);

        if (currentImageCount + files.size()
                > MAX_IMAGES_PER_ITEM) {
            throw new InvalidImageException(
                    "An item can have a maximum of "
                            + MAX_IMAGES_PER_ITEM
                            + " images"
            );
        }

        List<ItemImage> savedImages = new ArrayList<>();
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            int nextSortOrder = Math.toIntExact(currentImageCount);
            boolean makeFirstImagePrimary = currentImageCount == 0;

            for (MultipartFile file : files) {
                StoredImage storedImage = imageStorageService.uploadItemImage(file, itemId);
                uploadedPublicIds.add(storedImage.publicId());
                ItemImage itemImage = ItemImage.builder()
                                .item(item)
                                .imageUrl(storedImage.imageUrl())
                                .thumbnailUrl(storedImage.thumbnailUrl())
                                .publicId(storedImage.publicId())
                                .assetId(storedImage.assetId())
                                .sortOrder(nextSortOrder++)
                                .primary(makeFirstImagePrimary)
                                .build();

                makeFirstImagePrimary = false;

                savedImages.add(
                        itemImageRepository.save(itemImage)
                );
            }

            return savedImages
                    .stream()
                    .map(itemImageMapper::toResponse)
                    .toList();

        } catch (RuntimeException exception) {
            cleanupCloudinaryImages(uploadedPublicIds);
            throw exception;
        }
    }

    @Override
    public List<ItemImageResponse> getImages(UUID itemId) {
        Item item = itemRepository
                .findByIdAndDeletedFalse(itemId)
                .orElseThrow(
                        () -> new ItemNotFoundException(itemId)
                );

        return itemImageRepository
                .findAllByItemIdOrderBySortOrderAsc(item.getId())
                .stream()
                .map(itemImageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ItemImageResponse updateImage(UUID itemId, UUID imageId, UpdateItemImageRequest request, String authenticatedUserId) {
        getOwnedItem(itemId, authenticatedUserId);
        ItemImage image = getItemImage(itemId, imageId);

        if (request.sortOrder() != null) {
            image.setSortOrder(request.sortOrder());
        }

        if (Boolean.TRUE.equals(request.primary())) {
            changePrimaryImage(itemId, image);
        }

        ItemImage savedImage = itemImageRepository.save(image);

        return itemImageMapper.toResponse(savedImage);
    }

    @Override
    @Transactional
    public void deleteImage(UUID itemId, UUID imageId, String authenticatedUserId) {
        getOwnedItem(itemId, authenticatedUserId);

        ItemImage image = getItemImage(itemId, imageId);

        boolean wasPrimary = image.isPrimary();

        imageStorageService.deleteImage(image.getPublicId());

        itemImageRepository.delete(image);
        itemImageRepository.flush();

        if (wasPrimary) {
            assignNextPrimaryImage(itemId);
        }
    }

    private Item getOwnedItem(UUID itemId, String authenticatedUserId) {
        Item item = itemRepository
                .findByIdAndDeletedFalse(itemId)
                .orElseThrow(
                        () -> new ItemNotFoundException(itemId)
                );

        if (authenticatedUserId == null || !item.getOwnerId().equals(authenticatedUserId)) {
            throw new ItemAccessDeniedException();
        }

        return item;
    }

    private ItemImage getItemImage(
            UUID itemId,
            UUID imageId
    ) {
        return itemImageRepository
                .findByIdAndItemId(imageId, itemId)
                .orElseThrow(
                        () -> new ItemImageNotFoundException(imageId)
                );
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidImageException(
                    "At least one image is required"
            );
        }

        if (files.size() > MAX_IMAGES_PER_ITEM) {
            throw new InvalidImageException(
                    "You cannot upload more than "
                            + MAX_IMAGES_PER_ITEM
                            + " images at once"
            );
        }
    }

    private void changePrimaryImage(
            UUID itemId,
            ItemImage newPrimaryImage
    ) {
        itemImageRepository
                .findFirstByItemIdAndPrimaryTrue(itemId)
                .filter(currentPrimary -> !currentPrimary
                                        .getId()
                                        .equals(newPrimaryImage.getId())
                )
                .ifPresent(currentPrimary -> {
                    currentPrimary.setPrimary(false);
                    itemImageRepository.save(currentPrimary);
                });

        newPrimaryImage.setPrimary(true);
    }

    private void assignNextPrimaryImage(UUID itemId) {
        itemImageRepository
                .findFirstByItemIdOrderBySortOrderAsc(itemId)
                .ifPresent(nextImage -> {
                    nextImage.setPrimary(true);
                    itemImageRepository.save(nextImage);
                });
    }

    private void cleanupCloudinaryImages(List<String> publicIds) {
        for (String publicId : publicIds) {
            try {
                imageStorageService.deleteImage(publicId);
            } catch (RuntimeException ignored) {
            }
        }
    }
}