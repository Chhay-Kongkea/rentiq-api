package co.istad.rentiq_api.features.itemrequest.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.itemrequest.dto.request.CreateItemRequestRequest;
import co.istad.rentiq_api.features.itemrequest.dto.request.ItemRequestFilter;
import co.istad.rentiq_api.features.itemrequest.dto.request.NearbyItemRequestFilter;
import co.istad.rentiq_api.features.itemrequest.dto.request.UpdateItemRequestRequest;
import co.istad.rentiq_api.features.itemrequest.dto.response.ItemRequestResponse;
import co.istad.rentiq_api.features.itemrequest.service.ItemRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    @GetMapping("/item-requests")
    public PageResponse<ItemRequestResponse> getOpenRequests(
            @Valid
            @ModelAttribute
            ItemRequestFilter filter
    ) {
        return itemRequestService.getOpenRequests(filter);
    }

    @PostMapping("/item-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public ItemRequestResponse create(
            @Valid
            @RequestBody
            CreateItemRequestRequest request,

            Authentication authentication
    ) {
        return itemRequestService.create(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/item-requests/nearby")
    @PreAuthorize("hasRole('VENDOR')")
    public PageResponse<ItemRequestResponse> nearby(
            @Valid
            @ModelAttribute
            NearbyItemRequestFilter filter
    ) {
        return itemRequestService.getNearbyRequests(filter);
    }

    @GetMapping("/item-requests/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ItemRequestResponse getById(
            @PathVariable
            UUID requestId,

            Authentication authentication
    ) {
        return itemRequestService.getById(
                requestId,
                authentication.getName()
        );
    }

    @PatchMapping("/item-requests/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ItemRequestResponse update(
            @PathVariable
            UUID requestId,

            @Valid
            @RequestBody
            UpdateItemRequestRequest request,

            Authentication authentication
    ) {
        return itemRequestService.update(
                requestId,
                request,
                authentication.getName()
        );
    }

    @DeleteMapping("/item-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER')")
    public void cancel(
            @PathVariable
            UUID requestId,

            Authentication authentication
    ) {
        itemRequestService.cancel(
                requestId,
                authentication.getName()
        );
    }

    @GetMapping("/users/me/item-requests")
    @PreAuthorize("hasRole('USER')")
    public PageResponse<ItemRequestResponse> myRequests(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return itemRequestService.getMyRequests(
                authentication.getName(),
                pageNumber,
                pageSize
        );
    }
}