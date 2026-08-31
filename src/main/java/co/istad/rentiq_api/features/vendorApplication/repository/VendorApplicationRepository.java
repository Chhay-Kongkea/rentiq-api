package co.istad.rentiq_api.features.vendorApplication.repository;

import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {

    Optional<VendorApplication> findByUserId(String userId);

    Page<VendorApplication> findAllByStatus(VendorApplicationStatus status, Pageable pageable);

    long countByStatus(VendorApplicationStatus status);

    Page<VendorApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select count(application)
            from VendorApplication application, User user
            where application.userId = user.id
              and application.status = :applicationStatus
              and user.accountStatus = :accountStatus
            """)
    long countApprovedVendorsByAccountStatus(
            @Param("applicationStatus") VendorApplicationStatus applicationStatus,
            @Param("accountStatus") AccountStatus accountStatus);
}
