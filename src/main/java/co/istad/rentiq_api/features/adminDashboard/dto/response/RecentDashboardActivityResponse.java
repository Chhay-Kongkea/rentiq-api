package co.istad.rentiq_api.features.adminDashboard.dto.response;

import co.istad.rentiq_api.features.adminDashboard.enums.DashboardActivityType;

import java.time.OffsetDateTime;

public record RecentDashboardActivityResponse(
        DashboardActivityType type,
        String referenceId,
        String title,
        String status,
        OffsetDateTime createdAt
) {}
