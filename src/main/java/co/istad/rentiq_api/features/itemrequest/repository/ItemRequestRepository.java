package co.istad.rentiq_api.features.itemrequest.repository;
import co.istad.rentiq_api.features.itemrequest.entity.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, UUID>, JpaSpecificationExecutor<ItemRequest> {

    @EntityGraph(attributePaths = "offers")
    Optional<ItemRequest> findById(UUID id);

    @EntityGraph(attributePaths = "offers")
    Optional<ItemRequest> findByIdAndCustomerId(
            UUID id,
            String customerId
    );

    Page<ItemRequest>
    findAllByCustomerIdOrderByCreatedAtDesc(
            String customerId,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT r.*
                FROM item_requests r
                WHERE r.status = 'OPEN'
                  AND r.location IS NOT NULL
                  AND (
                        :categoryId IS NULL
                        OR r.category_id = :categoryId
                  )
                  AND (
                        r.expires_at IS NULL
                        OR r.expires_at > NOW()
                  )
                  AND ST_DWithin(
                        r.location,
                        ST_SetSRID(
                            ST_MakePoint(
                                :longitude,
                                :latitude
                            ),
                            4326
                        )::geography,
                        :radiusKm * 1000.0
                  )
                ORDER BY ST_Distance(
                    r.location,
                    ST_SetSRID(
                        ST_MakePoint(
                            :longitude,
                            :latitude
                        ),
                        4326
                    )::geography
                )
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM item_requests r
                WHERE r.status = 'OPEN'
                  AND r.location IS NOT NULL
                  AND (
                        :categoryId IS NULL
                        OR r.category_id = :categoryId
                  )
                  AND (
                        r.expires_at IS NULL
                        OR r.expires_at > NOW()
                  )
                  AND ST_DWithin(
                        r.location,
                        ST_SetSRID(
                            ST_MakePoint(
                                :longitude,
                                :latitude
                            ),
                            4326
                        )::geography,
                        :radiusKm * 1000.0
                  )
                """,
            nativeQuery = true
    )
    Page<ItemRequest> findNearbyOpenRequests(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("categoryId") UUID categoryId,
            Pageable pageable
    );
}
