package co.istad.rentiq_api.features.search.controller;


import co.istad.rentiq_api.features.item.dto.respone.ItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.search.dto.request.ItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.request.NearbyItemSearchFilter;
import co.istad.rentiq_api.features.search.dto.respone.SearchLogResponse;
import co.istad.rentiq_api.features.search.dto.respone.SearchSuggestionResponse;
import co.istad.rentiq_api.features.search.service.SearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/items")
    public PageResponse<ItemResponse> searchItems(
            @Valid
            @ModelAttribute
            ItemSearchFilter filter,
            Authentication authentication
    ) {
        return searchService.searchItems(filter, getOptionalUserId(authentication));
    }

    @GetMapping("/suggestions")
    public List<SearchSuggestionResponse> getSuggestions(

            @RequestParam
            @Size(min = 2, max = 100, message = "Keyword must contain between 2 and 100 characters")
            String keyword,

            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(20)
            Integer limit
    ) {
        return searchService.getSuggestions(keyword, limit);
    }

    @GetMapping("/nearby")
    public PageResponse<ItemResponse> searchNearby(
            @Valid
            @ModelAttribute
            NearbyItemSearchFilter filter,
            Authentication authentication
    ) {
        return searchService.searchNearby(filter, getOptionalUserId(authentication));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('USER')")
    public PageResponse<SearchLogResponse> getMyLogs(
            Authentication authentication,
            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return searchService.getMySearchLogs(
                authentication.getName(),
                pageNumber,
                pageSize
        );
    }

    private String getOptionalUserId(Authentication authentication) {
        if (authentication == null|| !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }
}