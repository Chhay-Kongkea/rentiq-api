package co.istad.rentiq_api.features.item.repository;

import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    @EntityGraph(attributePaths = "images")
    Optional<Item>
    findByIdAndDeletedFalseAndApprovalStatusAndStatus( UUID id, ItemApprovalStatus approvalStatus, ItemStatus status);

    @EntityGraph(attributePaths = "images")
    Optional<Item> findByIdAndDeletedFalse(UUID id);
}