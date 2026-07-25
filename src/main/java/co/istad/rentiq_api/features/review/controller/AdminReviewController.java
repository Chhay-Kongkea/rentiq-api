package co.istad.rentiq_api.features.review.controller;

import co.istad.rentiq_api.features.review.dto.response.ReviewResponse;
import co.istad.rentiq_api.features.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public Page<ReviewResponse> listReviews(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.adminListReviews(status, pageable);
    }

    @PatchMapping("/{id}/hide")
    public void hideReview(@PathVariable UUID id) {
        reviewService.adminHideReview(id);
    }

    @PatchMapping("/{id}/restore")
    public void restoreReview(@PathVariable UUID id) {
        reviewService.adminRestoreReview(id);
    }
}