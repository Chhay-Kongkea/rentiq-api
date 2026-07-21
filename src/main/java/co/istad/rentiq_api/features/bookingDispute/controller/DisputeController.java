package co.istad.rentiq_api.features.bookingDispute.controller;

import co.istad.rentiq_api.features.bookingDispute.dto.request.CreateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.request.UpdateDisputeRequest;
import co.istad.rentiq_api.features.bookingDispute.dto.response.DisputeResponse;
import co.istad.rentiq_api.features.bookingDispute.service.DisputeService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/bookings/{id}/disputes")
    public DisputeResponse createDispute(@PathVariable UUID id, @Valid @RequestBody CreateDisputeRequest request) {
        String userId = AuthUtils.extractUserId();
        return disputeService.createDispute(userId, id, request);
    }

    @GetMapping("/api/v1/bookings/{id}/disputes")
    public List<DisputeResponse> listDisputesForBooking(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return disputeService.listDisputesForBooking(userId, id);
    }

    @GetMapping("/api/v1/disputes/{id}")
    public DisputeResponse getDispute(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return disputeService.getDispute(userId, id);
    }

    @PatchMapping("/api/v1/disputes/{id}")
    public DisputeResponse updateDispute(@PathVariable UUID id, @Valid @RequestBody UpdateDisputeRequest request) {
        String userId = AuthUtils.extractUserId();
        return disputeService.updateDispute(userId, id, request);
    }
}