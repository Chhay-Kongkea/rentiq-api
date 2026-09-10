package co.istad.rentiq_api.features.adminDashboard.repository;

import co.istad.rentiq_api.features.adminDashboard.controller.AdminDashboardController;
import co.istad.rentiq_api.features.adminDashboard.dto.response.DashboardCountTrendPointResponse;
import co.istad.rentiq_api.features.adminDashboard.projection.DashboardCountProjection;
import co.istad.rentiq_api.features.adminDashboard.service.impl.AdminDashboardServiceImpl;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL and Hibernate parameter binding; no application data or external services. */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminDashboardTrendIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private LocalContainerEntityManagerFactoryBean factory;
    private EntityManager entityManager;
    private UserRepository users;
    private BookingRepository bookings;
    private AdminDashboardServiceImpl service;
    private MockMvc mvc;
    private final List<String> executedSql = new ArrayList<>();

    @BeforeAll
    void setUp() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        factory.setPackagesToScan("co.istad.rentiq_api");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "none",
                "hibernate.session_factory.statement_inspector", (StatementInspector) sql -> {
                    executedSql.add(sql);
                    return sql;
                }));
        factory.afterPropertiesSet();
        entityManager = factory.getObject().createEntityManager();
        // Only the columns read by the trend queries are needed; avoid unrelated PostGIS DDL.
        entityManager.getTransaction().begin();
        for (String table : List.of("users", "bookings")) {
            entityManager.createNativeQuery("create table " + table + " (created_at timestamptz not null)").executeUpdate();
            // Deliberately unsorted, with inclusive/exclusive boundaries and a UTC date rollover.
            entityManager.createNativeQuery("insert into " + table + " values "
                    + "('2026-03-01T12:00:00Z'), ('2026-01-01T00:00:00Z'),"
                    + "('2026-01-02T06:30:00+07:00'), ('2026-01-03T12:00:00Z'),"
                    + "('2025-12-31T23:59:59Z'), ('2026-03-02T00:00:00Z')").executeUpdate();
        }
        entityManager.getTransaction().commit();
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(entityManager);
        users = repositories.getRepository(UserRepository.class);
        bookings = repositories.getRepository(BookingRepository.class);
        service = new AdminDashboardServiceImpl(users, null, null, null, bookings, null, null, null, null);
        mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(service)).build();
    }

    @AfterAll
    void tearDown() {
        if (entityManager != null) entityManager.close();
        if (factory != null) factory.destroy();
    }

    @ParameterizedTest
    @CsvSource({"true,DAY", "true,MONTH", "false,DAY", "false,MONTH"})
    void repositoryAndServiceReturnOrderedCountsAcrossPeriods(boolean userGrowth, GroupBy groupBy) throws Exception {
        LocalDate from = LocalDate.parse("2026-01-01");
        LocalDate to = LocalDate.parse("2026-03-01");
        executedSql.clear();
        List<DashboardCountProjection> rows = query(userGrowth, groupBy, from, to);
        assertThat(executedSql).hasSize(1);
        assertThat(executedSql.getFirst())
                .contains("date_trunc(?,", "group by 1", "order by 1")
                .doesNotContain("group by date_trunc");
        assertThat(executedSql.getFirst().chars().filter(c -> c == '?').count()).isEqualTo(3);
        assertThat(rows).extracting(row -> row.getPeriod().toLocalDate())
                .containsExactlyElementsOf(groupBy == GroupBy.DAY
                        ? List.of(from, LocalDate.parse("2026-01-03"), to)
                        : List.of(from, to));
        assertThat(rows).extracting(DashboardCountProjection::getValue)
                .containsExactlyElementsOf(groupBy == GroupBy.DAY ? List.of(2L, 1L, 1L) : List.of(3L, 1L));

        var trend = userGrowth ? service.getUserGrowth(from, to, groupBy)
                : service.getBookingTrend(from, to, groupBy);
        assertThat(trend.groupBy()).isEqualTo(groupBy);
        assertThat(trend.data()).extracting(DashboardCountTrendPointResponse::period).isSorted();
        assertThat(trend.data()).hasSize(groupBy == GroupBy.DAY ? 60 : 3);
        assertThat(trend.data().getFirst().value()).isEqualTo(groupBy == GroupBy.DAY ? 2 : 3);
        assertThat(trend.data().get(1).value()).isZero();
        assertThat(trend.data().getLast().value()).isEqualTo(1);
        assertThat(trend.data().stream().mapToLong(DashboardCountTrendPointResponse::value).sum()).isEqualTo(4);

        // Exercise HTTP parameter conversion and period/value JSON with the real service/repositories.
        mvc.perform(get("/api/v1/admin/dashboard/" + (userGrowth ? "user-growth" : "booking-trend"))
                        .param("from", from.toString()).param("to", to.toString()).param("groupBy", groupBy.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].period").value(groupBy == GroupBy.DAY ? "2026-01-01" : "2026-01"))
                .andExpect(jsonPath("$.data[0].value").value(groupBy == GroupBy.DAY ? 2 : 3));
    }

    @ParameterizedTest
    @CsvSource({"true,DAY", "true,MONTH", "false,DAY", "false,MONTH"})
    void emptyRangeReturnsNoRepositoryRowsAndZeroFilledServiceSeries(boolean userGrowth, GroupBy groupBy) {
        LocalDate from = LocalDate.parse("2026-04-01");
        LocalDate to = LocalDate.parse("2026-04-03");
        assertThat(query(userGrowth, groupBy, from, to)).isEmpty();
        var trend = userGrowth ? service.getUserGrowth(from, to, groupBy)
                : service.getBookingTrend(from, to, groupBy);
        assertThat(trend.data()).hasSize(groupBy == GroupBy.DAY ? 3 : 1);
        assertThat(trend.data()).extracting(DashboardCountTrendPointResponse::period).isSorted();
        assertThat(trend.data()).allSatisfy(point -> assertThat(point.value()).isZero());
    }

    @Test
    void arbitraryGroupingIsRejectedBeforeQueryExecution() throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard/user-growth")
                        .param("from", "2026-01-01").param("groupBy", "day); drop table users; --"))
                .andExpect(status().isBadRequest());
    }

    private List<DashboardCountProjection> query(boolean userGrowth, GroupBy groupBy, LocalDate from, LocalDate to) {
        var start = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        var end = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        String unit = groupBy == GroupBy.MONTH ? "month" : "day";
        return userGrowth ? users.countRegistrationsByPeriod(start, end, unit)
                : bookings.countBookingsByPeriod(start, end, unit);
    }
}
