package co.istad.rentiq_api.features.adminDashboard.dto.response;

public record BookingDashboardSummaryResponse(
        long total,
        long active,
        long completed,
        long cancelled,
        long disputed
) {}
