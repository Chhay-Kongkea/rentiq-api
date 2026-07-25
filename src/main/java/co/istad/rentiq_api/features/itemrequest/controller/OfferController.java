package co.istad.rentiq_api.features.itemrequest.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.itemrequest.dto.request.CreateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.request.UpdateOfferRequest;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferResponse;
import co.istad.rentiq_api.features.itemrequest.dto.response.OfferStatusResponse;
import co.istad.rentiq_api.features.itemrequest.service.OfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/item-requests/{requestId}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse createOffer(
            @PathVariable UUID requestId,

            @Valid
            @RequestBody
            CreateOfferRequest request,

            Authentication authentication
    ) {
        return offerService.createOffer(
                requestId,
                request,
                authentication.getName()
        );
    }

    @GetMapping("/item-requests/{requestId}/offers")
    @PreAuthorize("hasRole('USER')")
    public List<OfferResponse> getRequestOffers(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        return offerService.getRequestOffers(
                requestId,
                authentication.getName()
        );
    }

    @PatchMapping("/item-requests/{requestId}/offers/{offerId}/accept")
    @PreAuthorize("hasRole('USER')")
    public OfferResponse acceptOffer(
            @PathVariable UUID requestId,
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        return offerService.acceptOffer(
                requestId,
                offerId,
                authentication.getName()
        );
    }

    @PatchMapping("/item-requests/{requestId}/offers/{offerId}/reject")
    @PreAuthorize("hasRole('USER')")
    public OfferResponse rejectOffer(
            @PathVariable UUID requestId,
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        return offerService.rejectOffer(
                requestId,
                offerId,
                authentication.getName()
        );
    }

    @GetMapping("/offers/{offerId}")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse getOfferById(
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        return offerService.getOfferById(
                offerId,
                authentication.getName()
        );
    }

    @PatchMapping("/offers/{offerId}")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferResponse updateOffer(
            @PathVariable UUID offerId,

            @Valid
            @RequestBody
            UpdateOfferRequest request,

            Authentication authentication
    ) {
        return offerService.updateOffer(
                offerId,
                request,
                authentication.getName()
        );
    }

    @DeleteMapping("/offers/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    public void withdrawOffer(
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        offerService.withdrawOffer(
                offerId,
                authentication.getName()
        );
    }

    @GetMapping("/vendors/me/offers")
    @PreAuthorize("hasRole('VENDOR')")
    public PageResponse<OfferResponse> getMyOffers(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return offerService.getMyOffers(
                authentication.getName(),
                pageNumber,
                pageSize
        );
    }

    @GetMapping("/vendors/me/offers/{offerId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public OfferStatusResponse getOfferStatus(
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        return offerService.getMyOfferStatus(
                offerId,
                authentication.getName()
        );
    }
}