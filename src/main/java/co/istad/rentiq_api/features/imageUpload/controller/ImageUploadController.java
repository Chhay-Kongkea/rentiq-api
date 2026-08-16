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

import java.util.UUID;

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

    @GetMapping("/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageUploadResponse> getImage(
            @PathVariable UUID imageId
    ) {
        return ResponseEntity.ok(
                imageUploadService.getById(imageId)
        );
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID imageId
    ) {
        imageUploadService.delete(imageId);

        return ResponseEntity.noContent().build();
    }
}
