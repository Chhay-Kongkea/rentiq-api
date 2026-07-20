package co.istad.rentiq_api.features.itemrequest.repository;

import co.istad.rentiq_api.features.itemrequest.entity.Offer;
import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository
        extends JpaRepository<Offer, UUID> {

    @EntityGraph(attributePaths = {
            "itemRequest",
            "item"
    })
    Optional<Offer> findByIdAndOwnerId(
            UUID offerId,
            String ownerId
    );

    @EntityGraph(attributePaths = {
            "itemRequest",
            "item"
    })
    List<Offer> findAllByItemRequestIdOrderByCreatedAtDesc(
            UUID requestId
    );

    @EntityGraph(attributePaths = {
            "itemRequest",
            "item"
    })
    Optional<Offer> findByIdAndItemRequestId(
            UUID offerId,
            UUID requestId
    );

    boolean existsByItemRequestIdAndOwnerId(
            UUID requestId,
            String ownerId
    );

    List<Offer> findAllByItemRequestIdAndStatus(
            UUID requestId,
            OfferStatus status
    );

    @EntityGraph(attributePaths = {
            "itemRequest",
            "item"
    })
    Page<Offer> findAllByOwnerIdOrderByCreatedAtDesc(
            String ownerId,
            Pageable pageable
    );
}