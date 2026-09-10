package co.istad.rentiq_api.features.imageUpload.validation;

import co.istad.rentiq_api.features.imageUpload.exception.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;
import java.util.function.Function;

import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.JPEG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.PNG_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.SVG_XSS_BYTES;
import static co.istad.rentiq_api.features.imageUpload.validation.ImageFixtures.WEBP_BYTES;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Backend audit P0-3 — SVG uploads must never be stored/served as a normal image (stored XSS:
 * an SVG can carry an embedded {@code <script>} that executes when the "image" is opened
 * directly). This is the single centralized validator every upload surface delegates to; it
 * must accept the three genuinely supported raster formats and reject everything else,
 * including SVG content wearing a PNG filename/Content-Type.
 */
class ImageContentValidatorTest {

    private static final long DEFAULT_MAX_SIZE = 10L * 1024 * 1024;
    private static final Function<String, RuntimeException> EXCEPTION_FACTORY = InvalidImageException::new;

    private final ImageContentValidator validator = new ImageContentValidator();

    @Test
    void acceptsJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        assertThatCode(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        assertThatCode(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsWebp() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", WEBP_BYTES);

        assertThatCode(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSvg_declaredHonestlyAsSvg() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.svg", "image/svg+xml", SVG_XSS_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsSvg_disguisedWithPngFilenameAndContentType() {
        // Attacker sets both the filename and the Content-Type header to look like a PNG.
        // Only sniffing the actual bytes (never trusting extension/header alone) catches this.
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", SVG_XSS_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsSvg_disguisedWithJpegFilenameAndContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", SVG_XSS_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, 10, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("size");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsNullFile() {
        assertThatThrownBy(() -> validator.validate(
                null, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsMissingContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", null, JPEG_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsNonImageContentType_evenWithImageBytes() {
        // Declared Content-Type is checked too (never rely on signature sniffing alone).
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "application/octet-stream", JPEG_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, ImageContentValidator.RASTER_IMAGE_TYPES, DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsContentTypeContentMismatch_pngHeaderClaimingJpegBytes() {
        // Declared type passes the allow-list, but the real bytes are a different (still
        // disallowed-for-this-caller-if-narrowed) format — signature is authoritative.
        MockMultipartFile file = new MockMultipartFile("file", "doc.jpg", "image/jpeg", PNG_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, Set.of("image/jpeg"), DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void respectsNarrowerCallerAllowList_rejectsWebpWhenOnlyJpegPngAllowed() {
        // e.g. KYC documents are restricted to JPEG/PNG only.
        MockMultipartFile file = new MockMultipartFile("file", "doc.webp", "image/webp", WEBP_BYTES);

        assertThatThrownBy(() -> validator.validate(
                file, Set.of("image/jpeg", "image/png"), DEFAULT_MAX_SIZE, EXCEPTION_FACTORY))
                .isInstanceOf(InvalidImageException.class);
    }
}
