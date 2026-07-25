package co.istad.rentiq_api.features.item.repository;

import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository
        extends JpaRepository<Item, UUID>,
        JpaSpecificationExecutor<Item> {

    @EntityGraph(attributePaths = "images")
    Optional<Item>
    findByIdAndDeletedFalseAndApprovalStatusAndStatus(
            UUID id,
            ItemApprovalStatus approvalStatus,
            ItemStatus status
    );

    @EntityGraph(attributePaths = "images")
    Optional<Item> findByIdAndDeletedFalse(UUID id);

    @Query("""
            SELECT DISTINCT item.title
            FROM Item item
            WHERE item.deleted = false
              AND item.approvalStatus =
                  co.istad.rentiq_api.features.item.enums.ItemApprovalStatus.APPROVED
              AND item.status =
                  co.istad.rentiq_api.features.item.enums.ItemStatus.ACTIVE
              AND item.available = true
              AND LOWER(item.title)
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY item.title ASC
            """)
    List<String> findTitleSuggestions(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT item.*
                    FROM items item
                    WHERE item.is_deleted = FALSE
                      AND item.approval_status = 'APPROVED'
                      AND item.status = 'ACTIVE'
                      AND item.is_available = TRUE
                      AND item.latitude IS NOT NULL
                      AND item.longitude IS NOT NULL
                      AND (
                          :categoryId IS NULL
                          OR item.category_id = :categoryId
                      )
                      AND ST_DWithin(
                          ST_SetSRID(
                              ST_MakePoint(
                                  item.longitude,
                                  item.latitude
                              ),
                              4326
                          )::geography,
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
                        ST_SetSRID(
                            ST_MakePoint(
                                item.longitude,
                                item.latitude
                            ),
                            4326
                        )::geography,
                        ST_SetSRID(
                            ST_MakePoint(
                                :longitude,
                                :latitude
                            ),
                            4326
                        )::geography
                    ) ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM items item
                    WHERE item.is_deleted = FALSE
                      AND item.approval_status = 'APPROVED'
                      AND item.status = 'ACTIVE'
                      AND item.is_available = TRUE
                      AND item.latitude IS NOT NULL
                      AND item.longitude IS NOT NULL
                      AND (
                          :categoryId IS NULL
                          OR item.category_id = :categoryId
                      )
                      AND ST_DWithin(
                          ST_SetSRID(
                              ST_MakePoint(
                                  item.longitude,
                                  item.latitude
                              ),
                              4326
                          )::geography,
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
    Page<Item> searchNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("categoryId") Short categoryId,
            Pageable pageable
    );
}