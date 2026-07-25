package co.istad.rentiq_api.features.report.repository;

import co.istad.rentiq_api.features.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {

    Page<Report> findAllByReporterIdOrderByCreatedAtDesc(String reporterId, Pageable pageable);

    @EntityGraph(attributePaths = "actions")
    Optional<Report> findByIdAndReporterId(UUID reportId, String reporterId);

    @Override
    @EntityGraph(attributePaths = "actions")
    Optional<Report> findById(UUID reportId);
}