package co.istad.rentiq_api.features.bookings.service.impl;


import co.istad.rentiq_api.features.bookings.entity.Booking;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class BookingDocumentGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    byte[] generateReceipt(Booking booking) {
        return generate("Payment Receipt", booking);
    }

    byte[] generateInvoice(Booking booking) {
        return generate("Invoice", booking);
    }

    private byte[] generate(String title, Booking booking) {
        try {
            Document document = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph("Booking Reference: " + booking.getBookingRef(), labelFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Customer ID: " + booking.getCustomerId(), textFont));
            document.add(new Paragraph("Vendor ID: " + booking.getOwnerId(), textFont));
            document.add(new Paragraph("Item: " + booking.getItem().getTitle(), textFont));
            document.add(new Paragraph("Rental Period: "
                    + booking.getRentalStart().format(DATE_FORMAT) + " - "
                    + booking.getRentalEnd().format(DATE_FORMAT)
                    + " (" + booking.getRentalDays() + " days)", textFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Price Per Day: " + formatMoney(booking, booking.getBookedPricePerDay()), textFont));
            document.add(new Paragraph("Subtotal: " + formatMoney(booking, booking.getSubtotal()), textFont));
            document.add(new Paragraph("Security Deposit: " + formatMoney(booking, booking.getSecurityDeposit()), textFont));
            document.add(new Paragraph("Total Amount: " + formatMoney(booking, booking.getTotalAmount()), labelFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Booking Status: " + booking.getStatus(), textFont));
            document.add(new Paragraph("Payment Status: " + booking.getPaymentStatus(), textFont));

            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate " + title + " PDF", e);
        }
    }

    private String formatMoney(Booking booking, java.math.BigDecimal amount) {
        return booking.getCurrency() + " " + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
