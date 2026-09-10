package co.istad.rentiq_api.features.kyc.service;

import co.istad.rentiq_api.features.imageUpload.validation.ImageContentValidator;
import co.istad.rentiq_api.features.kyc.exception.KycException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycImageStorageService {

    private final Cloudinary cloudinary;
    private final ImageContentValidator imageContentValidator;

    // KYC documents are deliberately restricted to JPEG/PNG only (narrower than the product's
    // general JPEG/PNG/WEBP raster support) — unchanged by the P0-3 fix, only the underlying
    // check is now signature-validated instead of trusting the Content-Type header alone.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024; // 8MB

    public String upload(String userId, String side, MultipartFile file) {

        validate(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "rentiq/kyc/" + userId,
                            "public_id", side + "-" + UUID.randomUUID(),
                            "resource_type", "image",
                            "type", "authenticated" // not publicly listable — sensitive document
                    )
            );
            return (String) result.get("secure_url");

        } catch (IOException e) {
            log.error("Failed to upload KYC {} image for user {}", side, userId, e);
            throw new KycException(HttpStatus.BAD_GATEWAY, "Failed to upload document image");
        }
    }

    private void validate(MultipartFile file) {
        imageContentValidator.validate(
                file,
                ALLOWED_CONTENT_TYPES,
                MAX_FILE_SIZE_BYTES,
                message -> new KycException(HttpStatus.BAD_REQUEST, message)
        );
    }
}