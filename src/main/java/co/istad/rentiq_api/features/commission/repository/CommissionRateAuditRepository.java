package co.istad.rentiq_api.features.commission.repository;

import co.istad.rentiq_api.features.commission.entity.CommissionRateAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommissionRateAuditRepository extends JpaRepository<CommissionRateAudit, UUID> {
}
