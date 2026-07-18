package co.istad.rentiq_api.features.offers.repository;

import co.istad.rentiq_api.features.offers.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByIdAndRequesterId(UUID id, String requesterId);

    Optional<Offer> findByIdAndVendorId(UUID id, String vendorId);

    List<Offer> findByVendorId(String vendorId);

}