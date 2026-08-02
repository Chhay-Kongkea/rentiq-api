package co.istad.rentiq_api.features.vendorPerformance.repository;

import co.istad.rentiq_api.features.vendorPerformance.entity.VendorStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VendorStatusAuditRepository extends JpaRepository<VendorStatusAudit, UUID> {
}
