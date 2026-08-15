package co.istad.rentiq_api.features.bookings.service;

import co.istad.rentiq_api.features.bookings.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.bookings.dto.request.QrScanRequest;
import co.istad.rentiq_api.features.bookings.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.bookings.dto.response.BookingQrCodeResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingStatusHistoryResponse;
import co.istad.rentiq_api.features.bookings.dto.response.PageResponse;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse create(CreateBookingRequest request, String customerId);

    List<BookingResponse> findMyBookings(String customerId);

    BookingResponse findById(UUID bookingId, String callerId, boolean isAdmin);

    List<BookingStatusHistoryResponse> getStatusHistory(UUID bookingId, String callerId, boolean isAdmin);

    BookingResponse updateStatus(UUID bookingId, UpdateBookingStatusRequest request, String callerId, boolean isAdmin);

    List<BookingResponse> findVendorBookings(String ownerId);

    List<BookingResponse> getVendorSchedule(String ownerId, LocalDate from, LocalDate to);

    BookingQrCodeResponse getOrCreateQrCode(UUID bookingId, String customerId);

    BookingResponse scanQrCode(QrScanRequest request, String vendorId);

    byte[] generateReceipt(UUID bookingId, String callerId, boolean isAdmin);

    byte[] generateInvoice(UUID bookingId, String callerId, boolean isAdmin);

    PageResponse<BookingResponse> findAllForAdmin(int pageNumber, int pageSize, BookingStatus status);

    BookingResponse findByIdForAdmin(UUID bookingId);

}
