package co.istad.rentiq_api.features.kyc.service;

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
        if (file == null || file.isEmpty()) {
            throw new KycException(HttpStatus.BAD_REQUEST, "Document image is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new KycException(HttpStatus.BAD_REQUEST, "Document image must be smaller than 8MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new KycException(HttpStatus.BAD_REQUEST, "Document image must be JPEG or PNG");
        }
    }
}