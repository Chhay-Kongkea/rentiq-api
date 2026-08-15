package co.istad.rentiq_api.features.commission.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.commission.dto.CommissionByCategoryProjection;
import co.istad.rentiq_api.features.commission.dto.request.UpdateCommissionRateRequest;
import co.istad.rentiq_api.features.commission.dto.response.CategoryCommissionSummary;
import co.istad.rentiq_api.features.commission.dto.response.CommissionRateResponse;
import co.istad.rentiq_api.features.commission.dto.response.CommissionReportResponse;
import co.istad.rentiq_api.features.commission.entity.CommissionRateAudit;
import co.istad.rentiq_api.features.commission.mapper.CommissionMapper;
import co.istad.rentiq_api.features.commission.repository.CommissionRateAuditRepository;
import co.istad.rentiq_api.features.commission.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private static final int MAX_REPORT_RANGE_YEARS = 1;

    private final CategoryRepository categoryRepository;
    private final BookingRepository bookingRepository;
    private final CommissionRateAuditRepository auditRepository;
    private final CommissionMapper commissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRateResponse> listCommissionRates() {
        return categoryRepository.findAll().stream()
                .map(commissionMapper::toRateResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommissionRateResponse updateCommissionRate(Integer categoryId, UpdateCommissionRateRequest request, String adminId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", categoryId));

        BigDecimal oldRate = category.getCommissionRate();
        BigDecimal newRate = request.commissionRate();

        category.setCommissionRate(newRate);
        categoryRepository.save(category);

        // Written in the same transaction as the rate change itself — a commission
        // rate can never be mutated without a matching audit row.
        auditRepository.save(CommissionRateAudit.builder()
                .categoryId(categoryId)
                .oldRate(oldRate)
                .newRate(newRate)
                .changedBy(adminId)
                .reason(request.reason())
                .build());

        return commissionMapper.toRateResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionReportResponse getCommissionReport(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new InvalidOperationException("from date is required");
        }

        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        if (effectiveTo.isBefore(from)) {
            throw new InvalidOperationException("from date must not be after to date");
        }

        if (effectiveTo.isAfter(from.plusYears(MAX_REPORT_RANGE_YEARS))) {
            throw new InvalidOperationException("Report date range cannot exceed " + MAX_REPORT_RANGE_YEARS + " year");
        }

        OffsetDateTime fromInclusive = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toExclusive = effectiveTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<CommissionByCategoryProjection> rows = bookingRepository.aggregateCommissionByCategory(
                PaymentStatus.RELEASED_TO_VENDOR, fromInclusive, toExclusive);

        Map<Integer, String> categoryNames = categoryRepository.findAllById(
                        rows.stream().map(row -> row.getCategoryId().intValue()).toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<CategoryCommissionSummary> byCategory = rows.stream()
                .map(row -> {
                    Integer categoryId = row.getCategoryId().intValue();
                    return new CategoryCommissionSummary(
                            categoryId,
                            categoryNames.getOrDefault(categoryId, "Unknown Category"),
                            row.getTotalCommission(),
                            row.getBookingCount());
                })
                .toList();

        BigDecimal totalCommission = byCategory.stream()
                .map(CategoryCommissionSummary::totalCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBookings = byCategory.stream()
                .mapToLong(CategoryCommissionSummary::bookingCount)
                .sum();

        return new CommissionReportResponse(from, effectiveTo, totalCommission, totalBookings, byCategory);
    }
}
