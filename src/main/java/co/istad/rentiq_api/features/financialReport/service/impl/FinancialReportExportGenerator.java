package co.istad.rentiq_api.features.financialReport.service.impl;

import co.istad.rentiq_api.features.financialReport.dto.response.RevenuePeriodRow;
import co.istad.rentiq_api.features.financialReport.dto.response.RevenueReportResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;


@Component
class FinancialReportExportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    byte[] generateRevenuePdf(RevenueReportResponse report) {
        try {
            Document document = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("Revenue Report", titleFont));
            document.add(new Paragraph(
                    "Period: " + report.from().format(DATE_FORMAT) + " - " + report.to().format(DATE_FORMAT),
                    textFont));
            document.add(new Paragraph("Group By: " + report.groupBy(), textFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Total Revenue: " + formatMoney(report.totalRevenue()), labelFont));
            document.add(new Paragraph("Total Bookings: " + report.totalBookings(), labelFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.addCell("Period");
            table.addCell("Revenue");
            table.addCell("Bookings");

            for (RevenuePeriodRow row : report.rows().getContent()) {
                table.addCell(row.period().toString());
                table.addCell(formatMoney(row.totalRevenue()));
                table.addCell(String.valueOf(row.bookingCount()));
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate revenue report PDF", e);
        }
    }

    byte[] generateRevenueXlsx(RevenueReportResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Revenue Report");

            int rowIndex = 0;

            Row summaryHeaderRow = sheet.createRow(rowIndex++);
            summaryHeaderRow.createCell(0).setCellValue("Total Revenue");
            summaryHeaderRow.createCell(1).setCellValue("Total Bookings");

            Row summaryRow = sheet.createRow(rowIndex++);
            summaryRow.createCell(0).setCellValue(toDouble(report.totalRevenue()));
            summaryRow.createCell(1).setCellValue(report.totalBookings());

            rowIndex++;

            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("Period");
            header.createCell(1).setCellValue("Revenue");
            header.createCell(2).setCellValue("Bookings");

            for (RevenuePeriodRow row : report.rows().getContent()) {
                Row dataRow = sheet.createRow(rowIndex++);
                setSanitizedString(dataRow.createCell(0), row.period().toString());
                dataRow.createCell(1).setCellValue(toDouble(row.totalRevenue()));
                dataRow.createCell(2).setCellValue(row.bookingCount());
            }

            for (int col = 0; col < 3; col++) {
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate revenue report xlsx", e);
        }
    }

    private void setSanitizedString(Cell cell, String value) {
        cell.setCellValue(sanitizeForXlsx(value));
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

    private String formatMoney(BigDecimal amount) {
        return "USD " + amount.setScale(2, RoundingMode.HALF_UP);
    }
}
