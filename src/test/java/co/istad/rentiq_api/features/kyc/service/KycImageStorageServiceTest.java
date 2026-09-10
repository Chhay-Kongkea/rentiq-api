package co.istad.rentiq_api.features.kyc.service;

import co.istad.rentiq_api.features.imageUpload.validation.ImageContentValidator;
import co.istad.rentiq_api.features.kyc.exception.KycException;
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
 * Backend audit P0-3 — KYC documents are sensitive and already had a narrower JPEG/PNG-only
 * allow-list, but (like every other upload surface) trusted only the client-supplied
 * Content-Type header, so an SVG declared as {@code image/png} previously passed straight
 * through to Cloudinary. Now delegates to the shared ImageContentValidator.
 */
class KycImageStorageServiceTest {

    private Cloudinary cloudinary;
    private KycImageStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
        service = new KycImageStorageService(cloudinary, new ImageContentValidator());
    }

    @Test
    void rejectsSvgDisguisedAsPng_neverReachesCloudinary() {
        MockMultipartFile file = new MockMultipartFile("file", "id-front.png", "image/png", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.upload("user-1", "front", file))
                .isInstanceOf(KycException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsSvgDeclaredAsSvg() {
        MockMultipartFile file = new MockMultipartFile("file", "id-front.svg", "image/svg+xml", SVG_XSS_BYTES);

        assertThatThrownBy(() -> service.upload("user-1", "front", file))
                .isInstanceOf(KycException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsWebp_kycStaysJpegPngOnly() {
        // KYC's narrower allow-list is a deliberate, pre-existing policy — unchanged by this fix.
        MockMultipartFile file = new MockMultipartFile("file", "id-front.webp", "image/webp", WEBP_BYTES);

        assertThatThrownBy(() -> service.upload("user-1", "front", file))
                .isInstanceOf(KycException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] oversized = new byte[9 * 1024 * 1024];
        System.arraycopy(JPEG_BYTES, 0, oversized, 0, JPEG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "id-front.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> service.upload("user-1", "front", file))
                .isInstanceOf(KycException.class);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void acceptsJpeg_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "id-front.jpg", "image/jpeg", JPEG_BYTES);

        service.upload("user-1", "front", file);

        verify(cloudinary.uploader()).upload(eq(JPEG_BYTES), any());
    }

    @Test
    void acceptsPng_proceedsToCloudinaryUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "id-front.png", "image/png", PNG_BYTES);

        service.upload("user-1", "front", file);

        verify(cloudinary.uploader()).upload(eq(PNG_BYTES), any());
    }
}
