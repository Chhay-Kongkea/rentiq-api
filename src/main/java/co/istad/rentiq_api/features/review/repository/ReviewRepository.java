package co.istad.rentiq_api.features.review.repository;

import co.istad.rentiq_api.features.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    Page<Review> findByItemIdAndStatus(UUID itemId, String status, Pageable pageable);

    Page<Review> findByReviewerId(String reviewerId, Pageable pageable);

    Page<Review> findByStatus(String status, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.itemId = :itemId AND r.status = 'VISIBLE'")
    BigDecimal calculateAverageRating(@Param("itemId") UUID itemId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.itemId = :itemId AND r.status = 'VISIBLE'")
    long countVisibleReviews(@Param("itemId") UUID itemId);

    @Modifying
    @Query(value = "UPDATE items SET average_rating = COALESCE(:avg, 0), total_reviews = :count WHERE id = :itemId",
            nativeQuery = true)
    void syncItemRatingStats(@Param("itemId") UUID itemId, @Param("avg") BigDecimal avg, @Param("count") long count);
}