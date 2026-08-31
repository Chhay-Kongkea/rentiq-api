package co.istad.rentiq_api.features.advertisement.controller;

import co.istad.rentiq_api.features.advertisement.dto.request.CreateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.UpdateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.response.AdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.dto.response.PublicAdvertisementResponse;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import co.istad.rentiq_api.features.advertisement.service.AdvertisementService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @GetMapping("/advertisements")
    public Page<PublicAdvertisementResponse> getActiveAdvertisements(
            @RequestParam(required = false) UUID itemId,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return advertisementService.getPublicAdvertisements(itemId, pageable);
    }

    @GetMapping("/advertisements/{id}")
    public PublicAdvertisementResponse getActiveAdvertisement(@PathVariable UUID id) {
        return advertisementService.getPublicAdvertisement(id);
    }

    @PostMapping("/advertisements")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdvertisementResponse createAdvertisement(@Valid @RequestBody CreateAdvertisementRequest request) {
        return advertisementService.create(request, AuthUtils.extractUserId());
    }

    @PatchMapping("/advertisements/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public AdvertisementResponse updateAdvertisement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdvertisementRequest request
    ) {
        return advertisementService.update(id, request, AuthUtils.extractUserId());
    }

    @DeleteMapping("/advertisements/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAdvertisement(@PathVariable UUID id) {
        advertisementService.cancel(id, AuthUtils.extractUserId());
    }

    @GetMapping("/vendors/me/advertisements")
    @PreAuthorize("hasRole('VENDOR')")
    public Page<AdvertisementResponse> getMyAdvertisements(
            @RequestParam(required = false) AdvertisementStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return advertisementService.getMyAdvertisements(AuthUtils.extractUserId(), status, pageable);
    }
}
