package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.features.financialReport.dto.GroupBy;
import co.istad.rentiq_api.features.financialReport.dto.response.CommissionTimeSeriesResponse;
import co.istad.rentiq_api.features.financialReport.dto.response.PlatformRevenueCurrencySummary;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenuePeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Renders the Admin Financial Report PDF/XLSX exports from data already computed by
 * {@link FinancialReportServiceImpl} (Booking GMV, Calculated Commission, Platform Revenue).
 * This class only formats and lays out those numbers — it never recalculates a financial value.
 */
@Component
class FinancialReportExportGenerator {

    private static final String LOGO_CLASSPATH_LOCATION = "static/images/rentiq-logo.png";
    private static final byte[] LOGO_BYTES = loadLogoBytes();

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'");

    // Rentiq brand red (see rentiq-admin app/globals.css --brand) at a restrained, print-safe strength.
    private static final Color BRAND_RED = new Color(0xFE, 0x12, 0x1A);
    private static final Color HEADING_GRAY = new Color(0x33, 0x33, 0x36);
    private static final Color MUTED_GRAY = new Color(0x6B, 0x6F, 0x76);
    private static final Color BORDER_GRAY = new Color(0xE2, 0xE4, 0xE8);
    private static final Color CARD_FILL = new Color(0xFA, 0xFA, 0xFB);
    private static final Color TABLE_HEADER_FILL = new Color(0x24, 0x24, 0x27);
    private static final Color TABLE_ROW_ALT_FILL = new Color(0xF6, 0xF7, 0xF8);

    private static byte[] loadLogoBytes() {
        try {
            return new ClassPathResource(LOGO_CLASSPATH_LOCATION).getInputStream().readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    // =================================================================================
    // PDF
    // =================================================================================

    byte[] generateRevenuePdf(RevenueExportData data) {
        try {
            // A4 portrait with professional print margins (points; ~1mm = 2.83pt):
            // left/right ~21mm, top ~30mm (room for the header), bottom ~25mm (room for the footer).
            Document document = new Document(PageSize.A4, 60f, 60f, 85f, 70f);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new FooterPageEvent());

            document.addTitle("Rentiq Financial Report");
            document.open();

            addHeader(document, data);
            addSummarySection(document, data);
            addPlatformRevenueSection(document, data);
            addPeriodBreakdownTable(document, data);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate revenue report PDF", e);
        }
    }

    private void addHeader(Document document, RevenueExportData data) throws Exception {
        RevenueReportResponse revenue = data.revenue();

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.1f, 1f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = loadLogoImage();
        if (logo != null) {
            logoCell.addElement(logo);
        } else {
            // Logo failure must never fail the whole export — fall back to text branding.
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND_RED);
            logoCell.addElement(new Paragraph("Rentiq", brandFont));
        }
        headerTable.addCell(logoCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, HEADING_GRAY);
        Font rangeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_RED);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_GRAY);

        Paragraph title = new Paragraph("FINANCIAL REPORT", titleFont);
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph range = new Paragraph(formatDateRange(revenue.from(), revenue.to()), rangeFont);
        range.setAlignment(Element.ALIGN_RIGHT);
        range.setSpacingBefore(2f);
        Paragraph generated = new Paragraph("Generated: " + data.generatedAt().format(TIMESTAMP_FORMAT), metaFont);
        generated.setAlignment(Element.ALIGN_RIGHT);
        generated.setSpacingBefore(4f);

        metaCell.addElement(title);
        metaCell.addElement(range);
        metaCell.addElement(generated);
        headerTable.addCell(metaCell);

        document.add(headerTable);

        LineSeparator rule = new LineSeparator(1.2f, 100f, BRAND_RED, Element.ALIGN_CENTER, -4);
        Paragraph ruleParagraph = new Paragraph();
        ruleParagraph.add(new Chunk(rule));
        ruleParagraph.setSpacingBefore(6f);
        ruleParagraph.setSpacingAfter(14f);
        document.add(ruleParagraph);
    }

    private Image loadLogoImage() {
        if (LOGO_BYTES == null) {
            return null;
        }
        try {
            Image image = Image.getInstance(LOGO_BYTES);
            image.scaleToFit(120f, 46f);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    private void addSummarySection(Document document, RevenueExportData data) {
        RevenueReportResponse revenue = data.revenue();
        CommissionTimeSeriesResponse commission = data.commission();

        document.add(sectionHeading("Executive Summary"));

        PdfPTable cards = new PdfPTable(2);
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(4f);
        cards.addCell(summaryCard("MARKETPLACE BOOKING VALUE (GMV)",
                formatAmount(revenue.totalBookingValue()),
                revenue.totalBookings() + " booking(s)"));
        cards.addCell(summaryCard("CALCULATED COMMISSION",
                "USD " + formatAmount(commission.totalCommission()),
                commission.totalBookings() + " booking(s)"));
        document.add(cards);

        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, MUTED_GRAY);
        Paragraph gmvNote = new Paragraph(
                "Marketplace Booking Value (GMV) is the total value of bookings arranged through Rentiq. "
                        + "Renters pay vendors directly — this figure is not Rentiq platform revenue.",
                noteFont);
        gmvNote.setSpacingBefore(2f);
        gmvNote.setSpacingAfter(4f);
        document.add(gmvNote);

        Paragraph commissionNote = new Paragraph(
                "Calculated Commission is computed on completed bookings; Rentiq does not collect rental "
                        + "payments, so this is a calculated figure, not cash collected by Rentiq.",
                noteFont);
        commissionNote.setSpacingAfter(14f);
        document.add(commissionNote);
    }

    private PdfPCell summaryCard(String label, String value, String subtitle) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED_GRAY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, HEADING_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_GRAY);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CARD_FILL);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(12f);
        cell.setPaddingLeft(14f);

        Paragraph labelP = new Paragraph(label, labelFont);
        Paragraph valueP = new Paragraph(value, valueFont);
        valueP.setSpacingBefore(6f);
        Paragraph subtitleP = new Paragraph(subtitle, subtitleFont);
        subtitleP.setSpacingBefore(3f);

        cell.addElement(labelP);
        cell.addElement(valueP);
        cell.addElement(subtitleP);
        return cell;
    }

    private void addPlatformRevenueSection(Document document, RevenueExportData data) {
        document.add(sectionHeading("Platform Revenue"));

        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, MUTED_GRAY);
        Paragraph note = new Paragraph(
                "Platform Revenue is Rentiq's own earned revenue: Promotion and Advertisement wallet charges "
                        + "only. Wallet top-ups are funding, not revenue, and are excluded. USD and KHR are "
                        + "reported separately and never combined.",
                noteFont);
        note.setSpacingAfter(8f);
        document.add(note);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1.3f, 1.3f, 1.3f});
        table.setHeaderRows(1);
        table.setSpacingAfter(14f);

        addHeaderCell(table, "Currency");
        addHeaderCell(table, "Promotion Revenue");
        addHeaderCell(table, "Advertisement Revenue");
        addHeaderCell(table, "Total Platform Revenue");

        List<PlatformRevenueCurrencySummary> currencies = data.platformRevenue().currencies();
        int rowIndex = 0;
        for (PlatformRevenueCurrencySummary currency : currencies) {
            boolean shaded = rowIndex % 2 == 1;
            addBodyCell(table, currency.currency(), Element.ALIGN_LEFT, shaded, true);
            addBodyCell(table, formatAmount(currency.promotionRevenue()), Element.ALIGN_RIGHT, shaded, false);
            addBodyCell(table, formatAmount(currency.advertisementRevenue()), Element.ALIGN_RIGHT, shaded, false);
            addBodyCell(table, formatAmount(currency.totalRevenue()), Element.ALIGN_RIGHT, shaded, true);
            rowIndex++;
        }
        document.add(table);
    }

    private void addPeriodBreakdownTable(Document document, RevenueExportData data) {
        RevenueReportResponse revenue = data.revenue();
        List<RevenuePeriodRow> rows = revenue.rows().getContent();
        if (rows.isEmpty()) {
            return;
        }

        document.add(sectionHeading("Booking Value by Period"));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 1.3f, 1f});
        table.setHeaderRows(1);

        addHeaderCell(table, "Period");
        addHeaderCell(table, "Booking Value");
        addHeaderCell(table, "Bookings");

        DateTimeFormatter periodFormat = revenue.groupBy() == GroupBy.MONTH ? MONTH_FORMAT : DAY_FORMAT;
        int rowIndex = 0;
        for (RevenuePeriodRow row : rows) {
            boolean shaded = rowIndex % 2 == 1;
            addBodyCell(table, row.period().format(periodFormat), Element.ALIGN_LEFT, shaded, false);
            addBodyCell(table, formatAmount(row.totalBookingValue()), Element.ALIGN_RIGHT, shaded, false);
            addBodyCell(table, String.valueOf(row.bookingCount()), Element.ALIGN_RIGHT, shaded, false);
            rowIndex++;
        }
        document.add(table);
    }

    private Paragraph sectionHeading(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, HEADING_GRAY);
        Paragraph heading = new Paragraph(text, font);
        heading.setSpacingBefore(4f);
        heading.setSpacingAfter(8f);
        return heading;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(TABLE_HEADER_FILL);
        cell.setPadding(7f);
        cell.setBorderColor(TABLE_HEADER_FILL);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, boolean shaded, boolean bold) {
        Font font = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 9.5f, HEADING_GRAY);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(shaded ? TABLE_ROW_ALT_FILL : Color.WHITE);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(6.5f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private String formatDateRange(LocalDate from, LocalDate to) {
        return from.format(DAY_FORMAT) + " – " + to.format(DAY_FORMAT);
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Footer with brand line + generated timestamp on the left, "Page N of TOTAL" on the right.
     * The total-page count uses the classic iText/OpenPDF placeholder-template technique since
     * the total isn't known until the document closes.
     */
    private static final class FooterPageEvent extends PdfPageEventHelper {

        private PdfTemplate totalPagesTemplate;
        private BaseFont footerBaseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(50, 20);
            try {
                footerBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                footerBaseFont = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (footerBaseFont == null) {
                return;
            }
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.setColorFill(MUTED_GRAY);
            cb.beginText();
            cb.setFontAndSize(footerBaseFont, 8f);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT,
                    "Rentiq — Generated by Rentiq Admin", document.left(), document.bottom() - 24, 0);

            String pageLabel = "Page " + writer.getPageNumber() + " of ";
            float pageLabelWidth = footerBaseFont.getWidthPoint(pageLabel, 8f);
            float x = document.right() - pageLabelWidth - 20;
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, pageLabel, x, document.bottom() - 24, 0);
            cb.endText();
            cb.addTemplate(totalPagesTemplate, x + pageLabelWidth, document.bottom() - 24);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (footerBaseFont == null) {
                return;
            }
            // At close, PdfWriter has already advanced its internal counter past the last
            // page that was actually written, so the true total is one less.
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(footerBaseFont, 8f);
            totalPagesTemplate.setColorFill(MUTED_GRAY);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPagesTemplate.endText();
        }
    }

    // =================================================================================
    // XLSX
    // =================================================================================

    byte[] generateRevenueXlsx(RevenueExportData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XlsxStyles styles = new XlsxStyles(workbook);
            buildSummarySheet(workbook, styles, data);
            buildPeriodDetailSheet(workbook, styles, data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate revenue report xlsx", e);
        }
    }

    private void buildSummarySheet(XSSFWorkbook workbook, XlsxStyles styles, RevenueExportData data) {
        Sheet sheet = workbook.createSheet("Summary");
        RevenueReportResponse revenue = data.revenue();
        CommissionTimeSeriesResponse commission = data.commission();

        int r = 0;
        Row titleRow = sheet.createRow(r++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Rentiq Financial Report");
        titleCell.setCellStyle(styles.title);

        r = writeLabelValueRow(sheet, styles, r, "Reporting Period",
                formatDateRange(revenue.from(), revenue.to()));
        r = writeLabelValueRow(sheet, styles, r, "Generated At",
                data.generatedAt().format(TIMESTAMP_FORMAT));
        r++;

        r = writeSectionHeader(sheet, styles, r, "Marketplace Booking Value (GMV)");
        r = writeHeaderRow(sheet, styles, r, "Metric", "Value");
        r = writeMetricRow(sheet, styles, r, "Total Booking Value", toDouble(revenue.totalBookingValue()), true);
        r = writeMetricCountRow(sheet, styles, r, "Total Bookings", revenue.totalBookings());
        r = writeNoteRow(sheet, styles, r,
                "GMV is the value of bookings arranged through Rentiq; renters pay vendors directly, so this is not Rentiq platform revenue.");
        r++;

        r = writeSectionHeader(sheet, styles, r, "Platform Revenue (Promotion + Advertisement wallet charges only)");
        r = writeHeaderRow(sheet, styles, r, "Currency", "Promotion Revenue", "Advertisement Revenue", "Total Platform Revenue");
        for (PlatformRevenueCurrencySummary currency : data.platformRevenue().currencies()) {
            Row row = sheet.createRow(r++);
            setString(row, 0, currency.currency(), styles.bodyLabel);
            setNumeric(row, 1, toDouble(currency.promotionRevenue()), styles.bodyNumber);
            setNumeric(row, 2, toDouble(currency.advertisementRevenue()), styles.bodyNumber);
            setNumeric(row, 3, toDouble(currency.totalRevenue()), styles.bodyNumberBold);
        }
        r++;

        r = writeSectionHeader(sheet, styles, r, "Calculated Commission");
        r = writeHeaderRow(sheet, styles, r, "Metric", "Value");
        r = writeMetricRow(sheet, styles, r, "Total Commission (USD)", toDouble(commission.totalCommission()), true);
        r = writeMetricCountRow(sheet, styles, r, "Total Bookings", commission.totalBookings());
        writeNoteRow(sheet, styles, r,
                "Calculated on completed bookings; Rentiq does not collect rental payments, so this is not cash collected by Rentiq.");

        sheet.setColumnWidth(0, 34 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 22 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.createFreezePane(0, 1);
    }

    private void buildPeriodDetailSheet(XSSFWorkbook workbook, XlsxStyles styles, RevenueExportData data) {
        RevenueReportResponse revenue = data.revenue();
        List<RevenuePeriodRow> rows = revenue.rows().getContent();
        if (rows.isEmpty()) {
            return;
        }

        Sheet sheet = workbook.createSheet("Booking Value Detail");
        boolean monthly = revenue.groupBy() == GroupBy.MONTH;
        CellStyle dateStyle = monthly ? styles.monthDate : styles.dayDate;

        int headerRowIndex = 0;
        Row header = sheet.createRow(headerRowIndex);
        setString(header, 0, "Period", styles.tableHeader);
        setString(header, 1, "Booking Value", styles.tableHeader);
        setString(header, 2, "Bookings", styles.tableHeader);

        int r = headerRowIndex + 1;
        for (RevenuePeriodRow row : rows) {
            Row dataRow = sheet.createRow(r++);
            Cell periodCell = dataRow.createCell(0);
            periodCell.setCellValue(Date.from(row.period().atStartOfDay(ZoneOffset.UTC).toInstant()));
            periodCell.setCellStyle(dateStyle);
            setNumeric(dataRow, 1, toDouble(row.totalBookingValue()), styles.bodyNumber);
            setNumeric(dataRow, 2, row.bookingCount(), styles.bodyNumber);
        }

        sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, headerRowIndex, 0, 2));
        sheet.createFreezePane(0, headerRowIndex + 1);
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 14 * 256);
    }

    private int writeSectionHeader(Sheet sheet, XlsxStyles styles, int rowIndex, String text) {
        Row row = sheet.createRow(rowIndex);
        setString(row, 0, text, styles.sectionHeader);
        return rowIndex + 1;
    }

    private int writeHeaderRow(Sheet sheet, XlsxStyles styles, int rowIndex, String... labels) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < labels.length; i++) {
            setString(row, i, labels[i], styles.tableHeader);
        }
        return rowIndex + 1;
    }

    private int writeLabelValueRow(Sheet sheet, XlsxStyles styles, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        setString(row, 0, label, styles.bodyLabelBold);
        setString(row, 1, value, styles.bodyLabel);
        return rowIndex + 1;
    }

    private int writeMetricRow(Sheet sheet, XlsxStyles styles, int rowIndex, String label, double value, boolean bold) {
        Row row = sheet.createRow(rowIndex);
        setString(row, 0, label, styles.bodyLabel);
        setNumeric(row, 1, value, bold ? styles.bodyNumberBold : styles.bodyNumber);
        return rowIndex + 1;
    }

    private int writeMetricCountRow(Sheet sheet, XlsxStyles styles, int rowIndex, String label, long value) {
        Row row = sheet.createRow(rowIndex);
        setString(row, 0, label, styles.bodyLabel);
        setNumeric(row, 1, value, styles.bodyNumber);
        return rowIndex + 1;
    }

    private int writeNoteRow(Sheet sheet, XlsxStyles styles, int rowIndex, String text) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(sanitizeForXlsx(text));
        cell.setCellStyle(styles.note);
        CellRangeAddress mergeRange = new CellRangeAddress(rowIndex, rowIndex, 0, 3);
        sheet.addMergedRegion(mergeRange);
        return rowIndex + 1;
    }

    private void setString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(sanitizeForXlsx(value));
        cell.setCellStyle(style);
    }

    private void setNumeric(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    static String sanitizeForXlsx(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

    private double toDouble(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Reusable cell styles for the workbook — built once per export, kept off the POI default style. */
    private static final class XlsxStyles {
        final CellStyle title;
        final CellStyle sectionHeader;
        final CellStyle tableHeader;
        final CellStyle bodyLabel;
        final CellStyle bodyLabelBold;
        final CellStyle bodyNumber;
        final CellStyle bodyNumberBold;
        final CellStyle note;
        final CellStyle dayDate;
        final CellStyle monthDate;

        XlsxStyles(XSSFWorkbook workbook) {
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());
            sectionHeader = workbook.createCellStyle();
            sectionHeader.setFont(sectionFont);
            sectionHeader.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            sectionHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            tableHeader = borderedStyle(workbook);
            tableHeader.setFont(headerFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            bodyLabel = borderedStyle(workbook);
            bodyLabelBold = borderedStyle(workbook);
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            bodyLabelBold.setFont(boldFont);

            bodyNumber = borderedStyle(workbook);
            bodyNumber.setAlignment(HorizontalAlignment.RIGHT);
            bodyNumber.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));

            bodyNumberBold = borderedStyle(workbook);
            bodyNumberBold.setAlignment(HorizontalAlignment.RIGHT);
            bodyNumberBold.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
            bodyNumberBold.setFont(boldFont);

            org.apache.poi.ss.usermodel.Font noteFont = workbook.createFont();
            noteFont.setItalic(true);
            noteFont.setFontHeightInPoints((short) 9);
            noteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            note = workbook.createCellStyle();
            note.setFont(noteFont);
            note.setWrapText(true);

            dayDate = borderedStyle(workbook);
            dayDate.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd mmm yyyy"));

            monthDate = borderedStyle(workbook);
            monthDate.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("mmm yyyy"));
        }

        private static CellStyle borderedStyle(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
