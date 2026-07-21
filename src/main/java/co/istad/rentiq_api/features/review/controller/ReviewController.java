package co.istad.rentiq_api.features.review.controller;

import co.istad.rentiq_api.features.review.dto.request.AttachReviewImagesRequest;
import co.istad.rentiq_api.features.review.dto.request.CreateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.UpdateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.VendorReplyRequest;
import co.istad.rentiq_api.features.review.dto.response.ReviewImageResponse;
import co.istad.rentiq_api.features.review.dto.response.ReviewResponse;
import co.istad.rentiq_api.features.review.service.ReviewService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/bookings/{id}/review")
    public ReviewResponse createReview(@PathVariable UUID id, @Valid @RequestBody CreateReviewRequest request) {
        String userId = AuthUtils.extractUserId();
        return reviewService.createReview(userId, id, request);
    }

    @GetMapping("/api/v1/items/{itemId}/reviews")
    public Page<ReviewResponse> getItemReviews(
            @PathVariable UUID itemId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.getItemReviews(itemId, pageable);
    }

    @GetMapping("/api/v1/reviews/{id}")
    public ReviewResponse getReview(@PathVariable UUID id) {
        return reviewService.getReview(id);
    }

    @PatchMapping("/api/v1/reviews/{id}")
    public ReviewResponse updateReview(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest request) {
        String userId = AuthUtils.extractUserId();
        return reviewService.updateReview(userId, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/reviews/{id}")
    public void deleteReview(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        reviewService.deleteReview(userId, id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/reviews/{id}/images")
    public List<ReviewImageResponse> attachImages(@PathVariable UUID id, @Valid @RequestBody AttachReviewImagesRequest request) {
        String userId = AuthUtils.extractUserId();
        return reviewService.attachImages(userId, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/reviews/{id}/images/{imageId}")
    public void removeImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        String userId = AuthUtils.extractUserId();
        reviewService.removeImage(userId, id, imageId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/reviews/{id}/reply")
    public ReviewResponse addReply(@PathVariable UUID id, @Valid @RequestBody VendorReplyRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return reviewService.addVendorReply(vendorId, id, request);
    }

    @PatchMapping("/api/v1/reviews/{id}/reply")
    public ReviewResponse editReply(@PathVariable UUID id, @Valid @RequestBody VendorReplyRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return reviewService.editVendorReply(vendorId, id, request);
    }

    @GetMapping("/api/v1/users/me/reviews")
    public Page<ReviewResponse> getMyReviews(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = AuthUtils.extractUserId();
        return reviewService.getMyReviews(userId, pageable);
    }
}