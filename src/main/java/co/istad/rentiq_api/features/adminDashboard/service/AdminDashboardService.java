package co.istad.rentiq_api.features.adminDashboard.service;

import co.istad.rentiq_api.features.adminDashboard.dto.response.AdminDashboardResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardCountTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.RecentDashboardActivityResponse;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {
    AdminDashboardResponse getDashboard();

    DashboardTrendResponse getRevenueTrend(LocalDate from, LocalDate to, GroupBy groupBy);

    DashboardCountTrendResponse getBookingTrend(LocalDate from, LocalDate to, GroupBy groupBy);

    DashboardCountTrendResponse getUserGrowth(LocalDate from, LocalDate to, GroupBy groupBy);

    List<RecentDashboardActivityResponse> getRecentActivity(int limit);
}
