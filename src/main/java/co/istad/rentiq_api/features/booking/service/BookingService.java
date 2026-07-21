package co.istad.rentiq_api.features.booking.service;

import co.istad.rentiq_api.features.booking.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.booking.dto.request.ScanQrRequest;
import co.istad.rentiq_api.features.booking.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.booking.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(String customerId, CreateBookingRequest request);

    Page<BookingResponse> listMyBookings(String customerId, Pageable pageable);

    BookingResponse getBooking(String userId, UUID bookingId);

    List<BookingStatusHistoryResponse> getStatusHistory(String userId, UUID bookingId);

    BookingResponse updateStatus(String userId, UUID bookingId, UpdateBookingStatusRequest request);

    Page<BookingResponse> listVendorBookings(String ownerId, Pageable pageable);

    List<BookingResponse> getVendorSchedule(String ownerId, LocalDate from, LocalDate to);

    QrCodeResponse getOrCreateQrCode(String userId, UUID bookingId);

    ScanResultResponse scanQrCode(String vendorId, ScanQrRequest request);

    ReceiptResponse getReceipt(String userId, UUID bookingId);

    InvoiceResponse getInvoice(String userId, UUID bookingId);

    Page<BookingResponse> adminListBookings(Pageable pageable);

    BookingResponse adminGetBooking(UUID bookingId);
}