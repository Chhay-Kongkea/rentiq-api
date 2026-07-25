package co.istad.rentiq_api.features.booking.controller;

import co.istad.rentiq_api.features.booking.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.booking.dto.request.ScanQrRequest;
import co.istad.rentiq_api.features.booking.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.booking.dto.response.*;
import co.istad.rentiq_api.features.booking.service.BookingService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        String userId = AuthUtils.extractUserId();
        return bookingService.createBooking(userId, request);
    }

    @GetMapping
    public Page<BookingResponse> listMyBookings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = AuthUtils.extractUserId();
        return bookingService.listMyBookings(userId, pageable);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return bookingService.getBooking(userId, id);
    }

    @GetMapping("/{id}/status-history")
    public List<BookingStatusHistoryResponse> getStatusHistory(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return bookingService.getStatusHistory(userId, id);
    }

    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateBookingStatusRequest request) {
        String userId = AuthUtils.extractUserId();
        return bookingService.updateStatus(userId, id, request);
    }

    @GetMapping("/{id}/qr-code")
    public QrCodeResponse getQrCode(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return bookingService.getOrCreateQrCode(userId, id);
    }

    @PostMapping("/qr-code/scan")
    public ScanResultResponse scanQrCode(@Valid @RequestBody ScanQrRequest request) {
        String vendorId = AuthUtils.extractUserId();
        return bookingService.scanQrCode(vendorId, request);
    }

    @GetMapping("/{id}/receipt")
    public ReceiptResponse getReceipt(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return bookingService.getReceipt(userId, id);
    }

    @GetMapping("/{id}/invoice")
    public InvoiceResponse getInvoice(@PathVariable UUID id) {
        String userId = AuthUtils.extractUserId();
        return bookingService.getInvoice(userId, id);
    }
}