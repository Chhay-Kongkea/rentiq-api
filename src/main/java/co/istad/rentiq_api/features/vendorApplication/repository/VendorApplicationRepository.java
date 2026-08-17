package co.istad.rentiq_api.features.vendorApplication.repository;

import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {

    Optional<VendorApplication> findByUserId(String userId);

    Page<VendorApplication> findAllByStatus(VendorApplicationStatus status, Pageable pageable);
}
