package co.istad.rentiq_api.features.adminDashboard.projection;

import java.sql.Date;

public interface DashboardCountProjection {
    Date getPeriod();
    Long getValue();
}
