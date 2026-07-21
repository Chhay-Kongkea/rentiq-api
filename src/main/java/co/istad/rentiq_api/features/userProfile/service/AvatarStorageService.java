package co.istad.rentiq_api.features.userProfile.service;



import co.istad.rentiq_api.features.userProfile.exception.AvatarStorageException;
import co.istad.rentiq_api.features.userProfile.exception.InvalidAvatarException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarStorageService {

    private final Cloudinary cloudinary;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    public String upload(String userId, MultipartFile file) {

        validate(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "rentiq/avatars",
                            "public_id", userId,
                            "overwrite", true,
                            "resource_type", "image",
                            "transformation", ObjectUtils.asMap(
                                    "width", 512, "height", 512, "crop", "fill", "gravity", "face"
                            )
                    )
            );
            String url = (String) result.get("secure_url");
            log.info("Avatar uploaded for user {}: {}", userId, url);
            return url;

        } catch (IOException e) {
            log.error("Failed to upload avatar for user {}", userId, e);
            throw new AvatarStorageException("Failed to upload avatar", e);
        }
    }

    public void delete(String userId) {
        try {
            cloudinary.uploader().destroy(
                    "rentiq/avatars/" + userId,
                    ObjectUtils.asMap("resource_type", "image")
            );
            log.info("Avatar deleted for user {}", userId);
        } catch (IOException e) {
            log.error("Failed to delete avatar for user {}", userId, e);
            throw new AvatarStorageException("Failed to delete avatar", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAvatarException("Avatar file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidAvatarException("Avatar must be smaller than 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarException("Avatar must be JPEG, PNG, or WEBP");
        }
    }
}