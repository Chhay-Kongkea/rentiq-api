package co.istad.rentiq_api.features.bookingDispute.controller;

import co.istad.rentiq_api.features.bookingDispute.dto.request.ResolveDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.response.DisputeResponse;
import co.istad.rentiq_api.features.bookingDispute.service.DisputeService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    public Page<DisputeResponse> listAll(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return disputeService.adminListDisputes(status, pageable);
    }

    @GetMapping("/{id}")
    public DisputeResponse getAny(@PathVariable UUID id) {
        return disputeService.adminGetDispute(id);
    }

    @PatchMapping("/{id}/status")
    public DisputeResponse resolve(@PathVariable UUID id, @Valid @RequestBody ResolveDisputeRequest request) {
        String adminId = AuthUtils.extractUserId();
        return disputeService.adminResolveDispute(adminId, id, request);
    }
}