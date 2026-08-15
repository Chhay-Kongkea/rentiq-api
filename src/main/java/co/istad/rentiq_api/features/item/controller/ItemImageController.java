package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.request.UpdateItemImageRequest;
import co.istad.rentiq_api.features.item.dto.respone.ItemImageResponse;
import co.istad.rentiq_api.features.item.service.ItemImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items/{itemId}/images")
@RequiredArgsConstructor
public class ItemImageController {


    private final ItemImageService itemImageService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<ItemImageResponse>> uploadImages(

            @PathVariable
            UUID itemId,

            @RequestPart("files")
            List<MultipartFile> files,

            Authentication authentication
    ) {
        List<ItemImageResponse> response =
                itemImageService.uploadImages(itemId, files, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ItemImageResponse>> getImages(
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(itemImageService.getImages(itemId));
    }

    @PatchMapping("/{imageId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ItemImageResponse> updateImage(

            @PathVariable
            UUID itemId,

            @PathVariable
            UUID imageId,

            @Valid
            @RequestBody
            UpdateItemImageRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                itemImageService.updateImage(itemId, imageId, request, authentication.getName())
        );
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Void> deleteImage(

            @PathVariable
            UUID itemId,

            @PathVariable
            UUID imageId,

            Authentication authentication
    ) {
        itemImageService.deleteImage(itemId, imageId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
