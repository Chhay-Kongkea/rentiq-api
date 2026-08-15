package co.istad.rentiq_api.features.imageUpload.controller;

import co.istad.rentiq_api.features.imageUpload.dto.response.ImageUploadResponse;
import co.istad.rentiq_api.features.imageUpload.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder
    ) {
        ImageUploadResponse response = imageUploadService.upload(file, folder);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageUploadResponse> getImage(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(imageUploadService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id
    ) {
        imageUploadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
