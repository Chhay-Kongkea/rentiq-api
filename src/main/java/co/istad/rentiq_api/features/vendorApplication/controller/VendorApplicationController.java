package co.istad.rentiq_api.features.vendorApplication.controller;

import co.istad.rentiq_api.features.vendorApplication.dto.request.SubmitVendorApplicationRequest;
import co.istad.rentiq_api.features.vendorApplication.dto.response.VendorApplicationResponse;
import co.istad.rentiq_api.features.vendorApplication.service.VendorApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendor-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class VendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    @PostMapping
    public ResponseEntity<VendorApplicationResponse> submit(
            @Valid @RequestBody(required = false) SubmitVendorApplicationRequest request,
            Authentication authentication
    ) {
        VendorApplicationResponse response = vendorApplicationService.submit(
                authentication.getName(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<VendorApplicationResponse> getMine(Authentication authentication) {
        return ResponseEntity.ok(
                vendorApplicationService.getMyApplication(authentication.getName())
        );
    }
}
