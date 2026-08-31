package co.istad.rentiq_api.features.imageUpload.controller;

import co.istad.rentiq_api.features.imageUpload.dto.response.ImageUploadResponse;
import co.istad.rentiq_api.features.imageUpload.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code GET}/{@code DELETE} by image ID were removed (backend audit SEC-004): the underlying
 * record has no owner field, so those endpoints let any authenticated user view or delete any
 * other user's uploaded image. No production feature depends on them — item images, avatars,
 * KYC documents, and review images each already have their own ownership-scoped upload/delete
 * path. Only upload remains.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;


    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestPart("image") MultipartFile image
    ) {
        ImageUploadResponse response =
                imageUploadService.upload(image, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
