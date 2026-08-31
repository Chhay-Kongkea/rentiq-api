package co.istad.rentiq_api.features.adminDashboard.dto.response;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;

import java.time.LocalDate;
import java.util.List;

public record DashboardTrendResponse(
        GroupBy groupBy,
        LocalDate from,
        LocalDate to,
        List<DashboardTrendPointResponse> data
) {}
