package co.istad.rentiq_api.features.commission.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.commission.dto.CommissionByCategoryProjection;
import co.istad.rentiq_api.features.commission.dto.request.UpdateCommissionRateRequest;
import co.istad.rentiq_api.features.commission.dto.response.CommissionReportResponse;
import co.istad.rentiq_api.features.commission.mapper.CommissionMapper;
import co.istad.rentiq_api.features.commission.repository.CommissionRateAuditRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceImplTest {

    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock private CategoryRepository categoryRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private CommissionRateAuditRepository auditRepository;
    @Mock private CommissionMapper commissionMapper;
    @Mock private AdminAuditService adminAuditService;

    private CommissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommissionServiceImpl(categoryRepository, bookingRepository, auditRepository, commissionMapper, adminAuditService);
    }

    @Test
    void updateCommissionRate_recordsOldAndNewRate() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setCommissionRate(new BigDecimal("0.1000"));

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        lenient().when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateCommissionRate(CATEGORY_ID, new UpdateCommissionRateRequest(new BigDecimal("0.1200"), "Market adjustment"), "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.COMMISSION_RATE_UPDATED,
                AdminAuditTargetType.CATEGORY,
                CATEGORY_ID.toString(),
                Map.of("commissionRate", new BigDecimal("0.1000")),
                Map.of("commissionRate", new BigDecimal("0.1200")),
                "Market adjustment");
    }

    // ---------------------------------------------------------------
    // Commission report (backend audit FIN-001) — commissionAmount is calculated per booking
    // but never actually collected via any wallet transaction, so the aggregate must never be
    // filtered by PaymentStatus (no booking ever reaches a "paid" status — rental payment is
    // P2P and never touches Rentiq).
    // ---------------------------------------------------------------

    @Test
    void getCommissionReport_aggregatesCalculatedCommission_withoutAnyPaymentStatusFilter() {
        CommissionByCategoryProjection row = mock(CommissionByCategoryProjection.class);
        when(row.getCategoryId()).thenReturn(CATEGORY_ID);
        when(row.getTotalCommission()).thenReturn(new BigDecimal("50.00"));
        when(row.getBookingCount()).thenReturn(4L);

        when(bookingRepository.aggregateCommissionByCategory(any(), any())).thenReturn(List.of(row));

        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("Electronics");
        when(categoryRepository.findAllById(List.of(CATEGORY_ID))).thenReturn(List.of(category));

        CommissionReportResponse response = service.getCommissionReport(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(response.totalCommission()).isEqualByComparingTo("50.00");
        assertThat(response.totalBookings()).isEqualTo(4L);
        assertThat(response.byCategory()).hasSize(1);
        assertThat(response.byCategory().get(0).categoryName()).isEqualTo("Electronics");
        // aggregateCommissionByCategory takes only (from, to) — no PaymentStatus parameter
        // exists on the repository method any more, so this call proves the fix by compiling.
        verify(bookingRepository).aggregateCommissionByCategory(any(), any());
    }
}
