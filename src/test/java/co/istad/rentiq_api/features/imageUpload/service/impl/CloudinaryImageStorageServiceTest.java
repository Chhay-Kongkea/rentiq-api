package co.istad.rentiq_api.features.imageUpload.service.impl;

import co.istad.rentiq_api.features.imageUpload.exception.InvalidImageException;
import co.istad.rentiq_api.features.imageUpload.validation.ImageContentValidator;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.JPEG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.PNG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.SVG_XSS_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.WEBP_BYTES;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Backend audit P0-3 — this is the shared storage path behind item images and the generic
 * {@code /api/v1/images/upload} endpoint (which review, advertisement, and inspection images
 * all upload their raw bytes through before referencing the resulting URL). Previously only
 * checked {@code contentType.startsWith("image/")}, which let {@code image/svg+xml} through.
 */
class CloudinaryImageStorageServiceTest {

    private Cloudinary cloudinary;
    private CloudinaryImageStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
        service = new CloudinaryImageStorageService(cloudinary, new ImageContentValidator());
    }

    @Test
    void rejectsSvgDeclaredAsSvg_neverReachesCloudinary() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.svg", "image/svg+xml", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.uploadImage(file, "rentiq/items/1"))
                .isInstanceOf(InvalidImageException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsSvgDisguisedAsPng_neverReachesCloudinary() {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.uploadImage(file, "rentiq/items/1"))
                .isInstanceOf(InvalidImageException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsOversizedFile_neverReachesCloudinary() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        System.arraycopy(JPEG_BYTES, 0, oversized, 0, JPEG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> service.uploadImage(file, "rentiq/items/1"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("size");

        verifyNoInteractions(cloudinary);
    }

    @Test
    void acceptsJpeg_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        service.uploadImage(file, "rentiq/items/1");

        org.mockito.Mockito.verify(cloudinary.uploader())
                .upload(org.mockito.ArgumentMatchers.eq(JPEG_BYTES), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsPng_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        service.uploadImage(file, "rentiq/items/1");

        org.mockito.Mockito.verify(cloudinary.uploader())
                .upload(org.mockito.ArgumentMatchers.eq(PNG_BYTES), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsWebp_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", WEBP_BYTES);

        service.uploadImage(file, "rentiq/items/1");

        org.mockito.Mockito.verify(cloudinary.uploader())
                .upload(org.mockito.ArgumentMatchers.eq(WEBP_BYTES), org.mockito.ArgumentMatchers.any());
    }
}
