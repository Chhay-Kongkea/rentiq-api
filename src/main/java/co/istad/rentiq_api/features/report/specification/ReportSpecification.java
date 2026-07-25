package co.istad.rentiq_api.features.report.specification;

import co.istad.rentiq_api.features.report.entity.Report;
import co.istad.rentiq_api.features.report.enums.ReportStatus;
import co.istad.rentiq_api.features.report.enums.ReportType;
import org.springframework.data.jpa.domain.Specification;

public final class ReportSpecification {

    private ReportSpecification() {
    }

    public static Specification<Report> adminFilter(ReportStatus status, ReportType reportType, String reporterId) {
        return Specification.allOf(
                statusEquals(status),
                typeEquals(reportType),
                reporterEquals(reporterId)
        );
    }

    private static Specification<Report> statusEquals(ReportStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("status"),
                        status
                );
    }

    private static Specification<Report> typeEquals(ReportType reportType) {
        return (root, query, cb) ->
                reportType == null
                        ? cb.conjunction()
                        : cb.equal(
                        root.get("reportType"),
                        reportType
                );
    }

    private static Specification<Report> reporterEquals(String reporterId) {
        return (root, query, cb) -> {
            if (reporterId == null
                    || reporterId.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("reporterId"),
                    reporterId.trim()
            );
        };
    }
}