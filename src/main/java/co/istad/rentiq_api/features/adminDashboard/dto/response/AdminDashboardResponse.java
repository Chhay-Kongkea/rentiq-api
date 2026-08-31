package co.istad.rentiq_api.features.adminDashboard.dto.response;

public record AdminDashboardResponse(
        UserDashboardSummaryResponse users,
        VendorDashboardSummaryResponse vendors,
        ListingDashboardSummaryResponse listings,
        BookingDashboardSummaryResponse bookings,
        PendingActionSummaryResponse pendingActions,
        FinancialDashboardSummaryResponse financial
) {}
