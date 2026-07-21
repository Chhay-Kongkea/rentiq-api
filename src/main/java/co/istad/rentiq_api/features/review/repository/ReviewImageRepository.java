package co.istad.rentiq_api.features.review.repository;

import co.istad.rentiq_api.features.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(UUID reviewId);

    Optional<ReviewImage> findByIdAndReviewId(UUID id, UUID reviewId);

    int countByReviewId(UUID reviewId);
}