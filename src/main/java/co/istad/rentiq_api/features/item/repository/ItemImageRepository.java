package co.istad.rentiq_api.features.item.repository;

import co.istad.rentiq_api.features.item.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {
    List<ItemImage> findAllByItemIdOrderBySortOrderAsc(UUID itemId);
    Optional<ItemImage> findByIdAndItemId(UUID imageId, UUID itemId);
    Optional<ItemImage> findFirstByItemIdAndPrimaryTrue(UUID itemId);
    Optional<ItemImage> findFirstByItemIdOrderBySortOrderAsc(UUID itemId);
    long countByItemId(UUID itemId);
}