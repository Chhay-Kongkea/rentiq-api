package co.istad.rentiq_api.features.adminDashboard.dto.response;

public record PendingActionSummaryResponse(
        long vendorApplications,
        long kycSubmissions,
        long listingApprovals,
        long disputes,
        long reports,
        long topUps
) {}
