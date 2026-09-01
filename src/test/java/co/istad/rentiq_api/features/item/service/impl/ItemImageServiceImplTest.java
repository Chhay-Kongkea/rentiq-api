package co.istad.rentiq_api.features.item.service.impl;

import co.istad.rentiq_api.features.imageUpload.dto.StoredImage;
import co.istad.rentiq_api.features.imageUpload.exception.InvalidImageException;
import co.istad.rentiq_api.features.imageUpload.service.ImageStorageService;
import co.istad.rentiq_api.features.item.dto.respone.ItemImageResponse;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.mapper.ItemImageMapper;
import co.istad.rentiq_api.features.item.repository.ItemImageRepository;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceImplTest {
    @Mock ItemRepository itemRepository;
    @Mock ItemImageRepository itemImageRepository;
    @Mock ImageStorageService imageStorageService;
    @Mock ItemImageMapper itemImageMapper;
    @Mock PlatformSettingService platformSettingService;
    ItemImageServiceImpl service;
    UUID itemId;
    MultipartFile file;

    @BeforeEach void setUp() {
        service = new ItemImageServiceImpl(itemRepository, itemImageRepository, imageStorageService,
                itemImageMapper, platformSettingService);
        itemId = UUID.randomUUID(); file = mock(MultipartFile.class);
        lenient().when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(
                Item.builder().id(itemId).ownerId("vendor-1").build()));
    }

    @Test void defaultLimitRemainsEight() {
        assertThat(PlatformSettingKey.LISTING_MAX_IMAGES.getDefaultValue()).isEqualByComparingTo("8");
    }

    @Test void uploadAtConfiguredMaximumSucceeds() {
        when(platformSettingService.getInteger(PlatformSettingKey.LISTING_MAX_IMAGES)).thenReturn(8);
        when(itemImageRepository.countByItemId(itemId)).thenReturn(7L);
        when(imageStorageService.uploadImage(file, "rentiq/items/" + itemId))
                .thenReturn(new StoredImage("url", "thumb", "public", "asset"));
        when(itemImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemImageMapper.toResponse(any())).thenReturn(mock(ItemImageResponse.class));
        assertThat(service.uploadImages(itemId, List.of(file), "vendor-1")).hasSize(1);
    }

    @Test void uploadBeyondConfiguredMaximumIsRejectedBeforeStorage() {
        when(platformSettingService.getInteger(PlatformSettingKey.LISTING_MAX_IMAGES)).thenReturn(5);
        when(itemImageRepository.countByItemId(itemId)).thenReturn(5L);
        assertThatThrownBy(() -> service.uploadImages(itemId, List.of(file), "vendor-1"))
                .isInstanceOf(InvalidImageException.class).hasMessageContaining("maximum of 5 images");
        verify(imageStorageService, never()).uploadImage(any(), any());
    }
}
