package co.istad.rentiq_api.features.adminDashboard.dto.response;

public record ListingDashboardSummaryResponse(
        long total,
        long active,
        long pendingApproval,
        long rejected,
        long featured
) {}
