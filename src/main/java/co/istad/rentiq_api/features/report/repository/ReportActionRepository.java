package co.istad.rentiq_api.features.report.repository;

import co.istad.rentiq_api.features.report.entity.ReportAction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportActionRepository extends JpaRepository<ReportAction, UUID> {

    @EntityGraph(attributePaths = "report")
    List<ReportAction> findAllByReportIdOrderByCreatedAtDesc(UUID reportId);
}