package co.istad.rentiq_api.features.review.service;

import co.istad.rentiq_api.features.review.dto.request.AttachReviewImagesRequest;
import co.istad.rentiq_api.features.review.dto.request.CreateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.UpdateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.VendorReplyRequest;
import co.istad.rentiq_api.features.review.dto.response.ReviewImageResponse;
import co.istad.rentiq_api.features.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(String userId, UUID bookingId, CreateReviewRequest request);

    Page<ReviewResponse> getItemReviews(UUID itemId, Pageable pageable);

    ReviewResponse getReview(UUID reviewId);

    ReviewResponse updateReview(String userId, UUID reviewId, UpdateReviewRequest request);

    void deleteReview(String userId, UUID reviewId);

    List<ReviewImageResponse> attachImages(String userId, UUID reviewId, AttachReviewImagesRequest request);

    void removeImage(String userId, UUID reviewId, UUID imageId);

    ReviewResponse addVendorReply(String vendorId, UUID reviewId, VendorReplyRequest request);

    ReviewResponse editVendorReply(String vendorId, UUID reviewId, VendorReplyRequest request);

    Page<ReviewResponse> getMyReviews(String userId, Pageable pageable);

    Page<ReviewResponse> adminListReviews(String statusFilter, Pageable pageable);

    void adminHideReview(UUID reviewId);

    void adminRestoreReview(UUID reviewId);
}