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

    Page<Review> findByItemIdAndStatus(
            UUID itemId,
            String status,
            Pageable pageable
    );

    Page<Review> findByReviewerId(
            String reviewerId,
            Pageable pageable
    );

    @Query("""
            SELECT r
            FROM Review r
            WHERE r.status = :status
            """)
    Page<Review> findByAccountStatus(
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.itemId = :itemId
              AND r.status = 'VISIBLE'
            """)
    BigDecimal calculateAverageRating(
            @Param("itemId") UUID itemId
    );

    @Query("""
            SELECT COUNT(r)
            FROM Review r
            WHERE r.itemId = :itemId
              AND r.status = 'VISIBLE'
            """)
    long countVisibleReviews(
            @Param("itemId") UUID itemId
    );

    @Modifying
    @Query(
            value = """
                    UPDATE items
                    SET average_rating = COALESCE(:avg, 0),
                        total_reviews = :count
                    WHERE id = :itemId
                    """,
            nativeQuery = true
    )
    void syncItemRatingStats(
            @Param("itemId") UUID itemId,
            @Param("avg") BigDecimal avg,
            @Param("count") long count
    );

    @Query(
            value = """
                    SELECT COALESCE(AVG(r.rating), 0)
                    FROM reviews r
                    JOIN items i ON i.id = r.item_id
                    WHERE i.owner_id = :ownerId
                      AND r.status = 'VISIBLE'
                    """,
            nativeQuery = true
    )
    BigDecimal calculateAverageRatingForOwner(
            @Param("ownerId") String ownerId
    );

    @Query(
            value = """
                    SELECT COUNT(r.id)
                    FROM reviews r
                    JOIN items i ON i.id = r.item_id
                    WHERE i.owner_id = :ownerId
                      AND r.status = 'VISIBLE'
                    """,
            nativeQuery = true
    )
    long countVisibleReviewsForOwner(
            @Param("ownerId") String ownerId
    );
}