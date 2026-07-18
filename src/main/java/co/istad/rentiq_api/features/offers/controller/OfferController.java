package co.istad.rentiq_api.features.offers.controller;


import co.istad.rentiq_api.features.item.dto.request.CreateItemRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferResponse;
import co.istad.rentiq_api.features.offers.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.offers.dto.response.OfferStatusResponse;
import co.istad.rentiq_api.features.offers.service.OfferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/item-requests/{requestId}/offers")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse createOffer(
            @PathVariable UUID requestId,
            @RequestBody @Valid CreateOfferRequest request,
            Authentication authentication) {

        return offerService.createOffer(
                requestId,
                request,
                authentication.getName());
    }

    @GetMapping("/offers/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse getOffer(@PathVariable UUID id){

        return offerService.getOffer(id);
    }

    @PatchMapping("/offers/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse updateOffer(
            @PathVariable UUID id,
            @RequestBody UpdateOfferRequest request,
            Authentication authentication){

        return offerService.updateOffer(
                id,
                request,
                authentication.getName());
    }

    @DeleteMapping("/offers/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public void deleteOffer(
            @PathVariable UUID id,
            Authentication authentication){

        offerService.deleteOffer(
                id,
                authentication.getName());
    }

    @GetMapping("/vendors/me/offers")
    @PreAuthorize("hasRole('VENDOR')")
    public List<OfferResponse> getMyOffers(
            Authentication authentication){

        return offerService.getVendorOffers(
                authentication.getName());
    }

    @GetMapping("/vendors/me/offers/{id}/status")
    @PreAuthorize("hasRole(" + "'VENDOR'" + ")")
    public OfferStatusResponse getOfferStatus(
            @PathVariable UUID id,
            Authentication authentication){

        return offerService.getOfferStatus(
                id,
                authentication.getName());
    }

}