package co.istad.rentiq_api.features.review.service.impl;

import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.item.entity.Item; // adjust package
import co.istad.rentiq_api.features.item.repository.ItemRepository; // adjust package
import co.istad.rentiq_api.features.review.dto.request.AttachReviewImagesRequest;
import co.istad.rentiq_api.features.review.dto.request.CreateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.ReviewImageInput;
import co.istad.rentiq_api.features.review.dto.request.UpdateReviewRequest;
import co.istad.rentiq_api.features.review.dto.request.VendorReplyRequest;
import co.istad.rentiq_api.features.review.dto.response.ReviewImageResponse;
import co.istad.rentiq_api.features.review.dto.response.ReviewResponse;
import co.istad.rentiq_api.features.review.entity.Review;
import co.istad.rentiq_api.features.review.entity.ReviewImage;
import co.istad.rentiq_api.features.review.exception.*;
import co.istad.rentiq_api.features.review.mapper.ReviewMapper;
import co.istad.rentiq_api.features.review.repository.ReviewImageRepository;
import co.istad.rentiq_api.features.review.repository.ReviewRepository;
import co.istad.rentiq_api.features.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String STATUS_VISIBLE = "VISIBLE";
    private static final String STATUS_HIDDEN = "HIDDEN";

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewMapper reviewMapper;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(String userId, UUID bookingId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ReviewBookingNotFoundException(bookingId));

        if (!booking.getCustomerId().equals(userId)) {
            throw new ReviewAccessDeniedException("Only the customer of this booking can leave a review");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BookingNotEligibleForReviewException(bookingId);
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new ReviewAlreadyExistsException(bookingId);
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .reviewerId(userId)
                .itemId(booking.getItem().getId())
                .rating(request.rating())
                .reviewText(request.reviewText())
                .status(STATUS_VISIBLE)
                .createdAt(Instant.now())
                .build();

        reviewRepository.save(review);
        recalculateItemStats(booking.getItem().getId());

        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getItemReviews(UUID itemId, Pageable pageable) {
        return reviewRepository.findByItemIdAndStatus(itemId, STATUS_VISIBLE, pageable).map(reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> STATUS_VISIBLE.equals(r.getStatus()))
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String userId, UUID reviewId, UpdateReviewRequest request) {
        Review review = getOwnedReview(userId, reviewId);

        if (request.rating() != null) review.setRating(request.rating());
        if (request.reviewText() != null) review.setReviewText(request.reviewText());

        reviewRepository.save(review);
        recalculateItemStats(review.getItemId());

        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(String userId, UUID reviewId) {
        Review review = getOwnedReview(userId, reviewId);
        UUID itemId = review.getItemId();
        reviewRepository.delete(review);
        recalculateItemStats(itemId);
    }

    @Override
    @Transactional
    public List<ReviewImageResponse> attachImages(String userId, UUID reviewId, AttachReviewImagesRequest request) {
        Review review = getOwnedReview(userId, reviewId);
        int startOrder = reviewImageRepository.countByReviewId(reviewId);

        List<ReviewImage> images = new ArrayList<>();
        List<ReviewImageInput> inputs = request.images();
        for (int i = 0; i < inputs.size(); i++) {
            ReviewImageInput input = inputs.get(i);
            images.add(ReviewImage.builder()
                    .reviewId(review.getId())
                    .imageUrl(input.imageUrl())
                    .thumbnailUrl(input.thumbnailUrl())
                    .sortOrder((short) (startOrder + i))
                    .createdAt(Instant.now())
                    .build());
        }
        reviewImageRepository.saveAll(images);

        return images.stream().map(reviewMapper::toImageResponse).toList();
    }

    @Override
    @Transactional
    public void removeImage(String userId, UUID reviewId, UUID imageId) {
        getOwnedReview(userId, reviewId);
        ReviewImage image = reviewImageRepository.findByIdAndReviewId(imageId, reviewId)
                .orElseThrow(() -> new ReviewImageNotFoundException(imageId));
        reviewImageRepository.delete(image);
    }

    @Override
    @Transactional
    public ReviewResponse addVendorReply(String vendorId, UUID reviewId, VendorReplyRequest request) {
        Review review = getReviewForVendor(vendorId, reviewId);
        if (review.getVendorReply() != null) {
            throw new VendorReplyAlreadyExistsException(reviewId);
        }
        review.setVendorReply(request.reply());
        review.setVendorRepliedAt(Instant.now());
        reviewRepository.save(review);
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse editVendorReply(String vendorId, UUID reviewId, VendorReplyRequest request) {
        Review review = getReviewForVendor(vendorId, reviewId);
        if (review.getVendorReply() == null) {
            throw new VendorReplyNotFoundException(reviewId);
        }
        review.setVendorReply(request.reply());
        review.setVendorRepliedAt(Instant.now());
        reviewRepository.save(review);
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String userId, Pageable pageable) {
        return reviewRepository.findByReviewerId(userId, pageable).map(reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> adminListReviews(String statusFilter, Pageable pageable) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return reviewRepository.findByStatus(statusFilter.toUpperCase(), pageable).map(reviewMapper::toResponse);
        }
        return reviewRepository.findAll(pageable).map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public void adminHideReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));
        review.setStatus(STATUS_HIDDEN);
        reviewRepository.save(review);
        recalculateItemStats(review.getItemId());
    }

    @Override
    @Transactional
    public void adminRestoreReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));
        review.setStatus(STATUS_VISIBLE);
        reviewRepository.save(review);
        recalculateItemStats(review.getItemId());
    }

    private Review getOwnedReview(String userId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getReviewerId().equals(userId)) {
            throw new ReviewAccessDeniedException("You do not own this review");
        }
        return review;
    }

    private Review getReviewForVendor(String vendorId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));
        Item item = itemRepository.findById(review.getItemId())
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!item.getOwnerId().equals(vendorId)) {
            throw new ReviewAccessDeniedException("Only the item owner can reply to this review");
        }
        return review;
    }

    private void recalculateItemStats(UUID itemId) {
        BigDecimal avg = reviewRepository.calculateAverageRating(itemId);
        long count = reviewRepository.countVisibleReviews(itemId);
        reviewRepository.syncItemRatingStats(itemId, avg, count);
    }
}