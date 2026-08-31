package co.istad.rentiq_api.features.adminDashboard.controller;

import co.istad.rentiq_api.features.adminDashboard.dto.response.AdminDashboardResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardCountTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardTrendResponse;
import co.istad.rentiq_api.features.adminDashboard.dto.response.RecentDashboardActivityResponse;
import co.istad.rentiq_api.features.adminDashboard.service.AdminDashboardService;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/revenue-trend")
    public DashboardTrendResponse getRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        return dashboardService.getRevenueTrend(from, to, groupBy);
    }

    @GetMapping("/booking-trend")
    public DashboardCountTrendResponse getBookingTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        return dashboardService.getBookingTrend(from, to, groupBy);
    }

    @GetMapping("/user-growth")
    public DashboardCountTrendResponse getUserGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy) {
        return dashboardService.getUserGrowth(from, to, groupBy);
    }

    @GetMapping("/recent-activity")
    public List<RecentDashboardActivityResponse> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentActivity(limit);
    }
}
