package co.istad.rentiq_api.features.item.repository;

import co.istad.rentiq_api.features.item.entity.ItemSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemSpecificationRepository extends JpaRepository<ItemSpecification, UUID> {
    List<ItemSpecification> findAllByItemIdOrderBySortOrderAsc(UUID itemId);
    Optional<ItemSpecification> findByIdAndItemId(UUID specificationId, UUID itemId);
    boolean existsByItemIdAndKeyIgnoreCase(UUID itemId, String key);
}