package co.istad.rentiq_api.features.bookingInspection.controller;

import co.istad.rentiq_api.features.bookingInspection.dto.request.AddInspectionImagesRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.request.UpsertInspectionRequest;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionImageResponse;
import co.istad.rentiq_api.features.bookingInspection.dto.response.InspectionResponse;
import co.istad.rentiq_api.features.bookingInspection.service.InspectionService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{id}/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public InspectionResponse createInspection(@PathVariable UUID id, @Valid @RequestBody UpsertInspectionRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return inspectionService.createInspection(vendorId, id, request);
    }

    @GetMapping
    public InspectionResponse getInspection(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return inspectionService.getInspection(userId, id);
    }

    @PatchMapping
    public InspectionResponse updateInspection(@PathVariable UUID id, @Valid @RequestBody UpsertInspectionRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return inspectionService.updateInspection(vendorId, id, request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/images")
    public List<InspectionImageResponse> addImages(@PathVariable UUID id, @Valid @RequestBody AddInspectionImagesRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return inspectionService.addImages(vendorId, id, request);
    }

    @GetMapping("/images")
    public List<InspectionImageResponse> listImages(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return inspectionService.listImages(userId, id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/images/{imageId}")
    public void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        String vendorId = AuthUtils.extractUserId();
        inspectionService.deleteImage(vendorId, id, imageId);
    }
}