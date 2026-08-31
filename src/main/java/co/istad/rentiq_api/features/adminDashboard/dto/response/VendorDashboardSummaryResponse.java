package co.istad.rentiq_api.features.adminDashboard.dto.response;

public record VendorDashboardSummaryResponse(
        long total,
        long active,
        long suspended,
        long banned,
        long pendingApplications
) {}
