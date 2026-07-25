package co.istad.rentiq_api.features.search.controller;

import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.search.dto.respone.SearchLogResponse;
import co.istad.rentiq_api.features.search.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/search-logs")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

    private final SearchService searchService;

    @GetMapping
    public PageResponse<SearchLogResponse> getAllSearchLogs(

            @RequestParam(defaultValue = "0")
            @Min(0)
            Integer pageNumber,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            Integer pageSize
    ) {
        return searchService.getAllSearchLogs(pageNumber, pageSize);
    }
}