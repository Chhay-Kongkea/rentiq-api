package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueSummaryResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenuePeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialReportExportGeneratorTest {

    private final FinancialReportExportGenerator generator = new FinancialReportExportGenerator();

    private RevenueExportData sampleData(GroupBy groupBy) {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 8);

        List<RevenuePeriodRow> periodRows = List.of(
                new RevenuePeriodRow(LocalDate.of(2026, 9, 1), new BigDecimal("1200.00"), 4),
                new RevenuePeriodRow(LocalDate.of(2026, 9, 2), new BigDecimal("980.50"), 3),
                new RevenuePeriodRow(LocalDate.of(2026, 9, 3), BigDecimal.ZERO, 0)
        );
        Page<RevenuePeriodRow> periodPage = new PageImpl<>(periodRows);
        RevenueReportResponse revenue = new RevenueReportResponse(
                from, to, groupBy, new BigDecimal("25200.00"), 42, periodPage);

        CommissionTimeSeriesResponse commission = new CommissionTimeSeriesResponse(
                from, to, groupBy, new BigDecimal("1260.00"), 42, new PageImpl<>(List.of()));

        List<PlatformRevenueCurrencySummary> currencies = List.of(
                new PlatformRevenueCurrencySummary("USD", new BigDecimal("1240.00"),
                        new BigDecimal("480.00"), new BigDecimal("760.00"), 30, 12, 18),
                new PlatformRevenueCurrencySummary("KHR", BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0)
        );
        OffsetDateTime fromOdt = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime toOdt = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        PlatformRevenueSummaryResponse platformRevenue =
                new PlatformRevenueSummaryResponse(fromOdt, toOdt, currencies);

        return new RevenueExportData(revenue, commission, platformRevenue,
                OffsetDateTime.of(2026, 9, 8, 14, 32, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void generateRevenuePdf_producesNonEmptyValidPdf() throws Exception {
        byte[] pdf = generator.generateRevenuePdf(sampleData(GroupBy.DAY));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // Locks in the "Page N of TOTAL" footer template fix: PdfWriter's internal page
        // counter is one ahead of the actual page count by the time onCloseDocument fires.
        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
        } finally {
            reader.close();
        }
    }

    @Test
    void generateRevenuePdf_handlesMonthGroupingAndEmptyRows() {
        RevenueExportData monthData = sampleData(GroupBy.MONTH);
        byte[] pdf = generator.generateRevenuePdf(monthData);
        assertThat(pdf).isNotEmpty();
    }

    @Test
    void generateRevenueXlsx_producesReadableWorkbookWithNumericCells() throws Exception {
        byte[] xlsx = generator.generateRevenueXlsx(sampleData(GroupBy.DAY));
        assertThat(xlsx).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            Sheet summary = workbook.getSheet("Summary");
            assertThat(summary).isNotNull();
            Sheet detail = workbook.getSheet("Booking Value Detail");
            assertThat(detail).isNotNull();
            // Booking Value column on the detail sheet must be a real numeric cell, not text.
            double firstBookingValue = detail.getRow(1).getCell(1).getNumericCellValue();
            assertThat(firstBookingValue).isEqualTo(1200.00);
        }
    }
}
