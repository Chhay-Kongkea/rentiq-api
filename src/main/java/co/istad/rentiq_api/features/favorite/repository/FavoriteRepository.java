package co.istad.rentiq_api.features.favorite.repository;

import co.istad.rentiq_api.features.favorite.entity.Favorite;
import co.istad.rentiq_api.features.favorite.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    Page<Favorite> findByIdUserId(String userId, Pageable pageable);

    boolean existsByIdUserIdAndIdItemId(String userId, UUID itemId);

    void deleteByIdUserIdAndIdItemId(String userId, UUID itemId);

    // Keeps items.favorite_count in sync without depending on the exact
    // field name used inside the Item entity.
    @Modifying
    @Query(value = "UPDATE items SET favorite_count = favorite_count + 1 WHERE id = :itemId", nativeQuery = true)
    void incrementFavoriteCount(@Param("itemId") UUID itemId);

    @Modifying
    @Query(value = "UPDATE items SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = :itemId", nativeQuery = true)
    void decrementFavoriteCount(@Param("itemId") UUID itemId);
}