package co.istad.rentiq_api.features.bookings.controller;

import co.istad.rentiq_api.features.bookings.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.bookings.dto.request.QrScanRequest;
import co.istad.rentiq_api.features.bookings.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.bookings.dto.response.BookingQrCodeResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingStatusHistoryResponse;
import co.istad.rentiq_api.features.bookings.dto.response.PageResponse;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication
    ) {
        return bookingService.create(request, authentication.getName());
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.findMyBookings(authentication.getName());
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse getBooking(@PathVariable UUID id, Authentication authentication) {
        return bookingService.findById(id, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/bookings/{id}/status-history")
    public List<BookingStatusHistoryResponse> getStatusHistory(@PathVariable UUID id, Authentication authentication) {
        return bookingService.getStatusHistory(id, authentication.getName(), isAdmin(authentication));
    }

    @PatchMapping("/bookings/{id}/status")
    public BookingResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookingStatusRequest request,
            Authentication authentication
    ) {
        return bookingService.updateStatus(id, request, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/bookings/{id}/qr-code")
    public BookingQrCodeResponse getQrCode(@PathVariable UUID id, Authentication authentication) {
        return bookingService.getOrCreateQrCode(id, authentication.getName());
    }

    @PostMapping("/bookings/qr-code/scan")
    @PreAuthorize("hasRole('VENDOR')")
    public BookingResponse scanQrCode(@Valid @RequestBody QrScanRequest request, Authentication authentication) {
        return bookingService.scanQrCode(request, authentication.getName());
    }

    @GetMapping(value = "/bookings/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(@PathVariable UUID id, Authentication authentication) {
        byte[] pdf = bookingService.generateReceipt(id, authentication.getName(), isAdmin(authentication));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/bookings/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoice(@PathVariable UUID id, Authentication authentication) {
        byte[] pdf = bookingService.generateInvoice(id, authentication.getName(), isAdmin(authentication));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/vendors/me/bookings")
    @PreAuthorize("hasRole('VENDOR')")
    public List<BookingResponse> getVendorBookings(Authentication authentication) {
        return bookingService.findVendorBookings(authentication.getName());
    }

    @GetMapping("/vendors/me/bookings/schedule")
    @PreAuthorize("hasRole('VENDOR')")
    public List<BookingResponse> getVendorSchedule(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return bookingService.getVendorSchedule(authentication.getName(), from, to);
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<BookingResponse> getAllBookings(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) BookingStatus status
    ) {
        return bookingService.findAllForAdmin(pageNumber, pageSize, status);
    }

    @GetMapping("/admin/bookings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse getBookingForAdmin(@PathVariable UUID id) {
        return bookingService.findByIdForAdmin(id);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
