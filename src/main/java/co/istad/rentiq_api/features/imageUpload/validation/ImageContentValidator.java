package co.istad.rentiq_api.features.imageUpload.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Backend audit P0-3 — single source of truth for what counts as an acceptable uploaded image
 * across every upload surface (generic uploads, item images, KYC documents, avatars). Every
 * feature-specific storage service (Cloudinary-backed item/generic images, KYC documents,
 * avatars) delegates here instead of re-implementing its own content-type check, so SVG (or any
 * other non-raster content) cannot slip back in through one path while being blocked on another.
 *
 * Trusts neither the filename extension nor the client-supplied Content-Type header alone: the
 * declared Content-Type must be on the caller's allow-list AND the file's own magic-byte
 * signature must independently resolve to one of the same allowed raster formats. SVG is an
 * XML/text format — it never produces a JPEG/PNG/WEBP signature — so a request that declares
 * {@code image/png} but actually contains SVG/script markup is rejected by the signature check
 * even though the header lied.
 */
@Component
public class ImageContentValidator {

    /** The only raster formats genuinely supported by the product's image pipeline. */
    public static final Set<String> RASTER_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final int SIGNATURE_SNIFF_LENGTH = 512;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP = {0x57, 0x45, 0x42, 0x50};

    /**
     * @param allowedContentTypes the caller's allowed set (a subset of, or equal to, {@link #RASTER_IMAGE_TYPES})
     * @param maxSizeBytes        the caller's existing size limit — unchanged per feature, not unified
     * @param exceptionFactory    builds the caller's own exception type from a message, so each
     *                            upload surface keeps returning its existing error response shape
     */
    public void validate(
            MultipartFile file,
            Set<String> allowedContentTypes,
            long maxSizeBytes,
            Function<String, ? extends RuntimeException> exceptionFactory
    ) {
        if (file == null || file.isEmpty()) {
            throw exceptionFactory.apply("Image file is required");
        }

        if (file.getSize() > maxSizeBytes) {
            throw exceptionFactory.apply(
                    "Image size must not exceed " + (maxSizeBytes / (1024 * 1024)) + "MB");
        }

        String declaredContentType = normalize(file.getContentType());
        if (declaredContentType == null || !allowedContentTypes.contains(declaredContentType)) {
            throw exceptionFactory.apply(unsupportedTypeMessage(allowedContentTypes));
        }

        byte[] header = readHeader(file, exceptionFactory);
        String detectedContentType = detectSignature(header);

        // The authoritative check: never trust the extension or the client-supplied
        // Content-Type alone. The bytes themselves must independently match one of the
        // allowed raster formats — this is what catches a .png-named/labelled file whose
        // actual content is SVG (or anything else that isn't a real raster image).
        if (detectedContentType == null || !allowedContentTypes.contains(detectedContentType)) {
            throw exceptionFactory.apply(unsupportedTypeMessage(allowedContentTypes));
        }
    }

    private byte[] readHeader(MultipartFile file, Function<String, ? extends RuntimeException> exceptionFactory) {
        try (InputStream in = file.getInputStream()) {
            byte[] buffer = new byte[SIGNATURE_SNIFF_LENGTH];
            int read = in.readNBytes(buffer, 0, buffer.length);
            return read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
        } catch (IOException e) {
            throw exceptionFactory.apply("Unable to read image file");
        }
    }

    private String detectSignature(byte[] header) {
        if (startsWith(header, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        if (startsWith(header, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (isWebp(header)) {
            return "image/webp";
        }
        if (looksLikeSvg(header)) {
            return "image/svg+xml";
        }
        return null;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && startsWith(header, RIFF)
                && header[8] == WEBP[0]
                && header[9] == WEBP[1]
                && header[10] == WEBP[2]
                && header[11] == WEBP[3];
    }

    private boolean looksLikeSvg(byte[] header) {
        String text = new String(header, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return text.contains("<svg");
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String contentType) {
        if (contentType == null) {
            return null;
        }
        int separator = contentType.indexOf(';');
        String base = (separator >= 0 ? contentType.substring(0, separator) : contentType)
                .trim().toLowerCase(Locale.ROOT);
        return base.isBlank() ? null : base;
    }

    private String unsupportedTypeMessage(Set<String> allowedContentTypes) {
        return "Only the following image types are allowed: " + String.join(", ", new TreeSet<>(allowedContentTypes));
    }
}
