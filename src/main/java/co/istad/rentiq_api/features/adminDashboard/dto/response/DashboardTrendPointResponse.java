package co.istad.rentiq_api.features.adminDashboard.dto.response;

import java.math.BigDecimal;

public record DashboardTrendPointResponse(String period, BigDecimal value) {}
