package co.istad.rentiq_api.features.userProfile.service;

import co.istad.rentiq_api.features.imageUpload.validation.ImageContentValidator;
import co.istad.rentiq_api.features.userProfile.exception.InvalidAvatarException;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.JPEG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.PNG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.SVG_XSS_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.WEBP_BYTES;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Backend audit P0-3 — avatars are rendered directly in profile UIs across the product, making
 * this one of the higher-visibility stored-XSS surfaces if an SVG got through.
 */
class AvatarStorageServiceTest {

    private Cloudinary cloudinary;
    private AvatarStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
        service = new AvatarStorageService(cloudinary, new ImageContentValidator());
    }

    @Test
    void rejectsSvgDisguisedAsPng_neverReachesCloudinary() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.upload("user-1", file))
                .isInstanceOf(InvalidAvatarException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsSvgDeclaredAsSvg() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.svg", "image/svg+xml", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.upload("user-1", file))
                .isInstanceOf(InvalidAvatarException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] oversized = new byte[6 * 1024 * 1024];
        System.arraycopy(JPEG_BYTES, 0, oversized, 0, JPEG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> service.upload("user-1", file))
                .isInstanceOf(InvalidAvatarException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void acceptsJpeg_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", JPEG_BYTES);

        service.upload("user-1", file);

        verify(cloudinary.uploader()).upload(eq(JPEG_BYTES), any());
    }

    @Test
    void acceptsPng_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", PNG_BYTES);

        service.upload("user-1", file);

        verify(cloudinary.uploader()).upload(eq(PNG_BYTES), any());
    }

    @Test
    void acceptsWebp_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.webp", "image/webp", WEBP_BYTES);

        service.upload("user-1", file);

        verify(cloudinary.uploader()).upload(eq(WEBP_BYTES), any());
    }
}
