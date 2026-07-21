package co.istad.rentiq_api.features.booking.controller;

import co.istad.rentiq_api.features.booking.dto.response.BookingResponse;
import co.istad.rentiq_api.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public Page<BookingResponse> listAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingService.adminListBookings(pageable);
    }

    @GetMapping("/{id}")
    public BookingResponse getAny(@PathVariable UUID id) {
        return bookingService.adminGetBooking(id);
    }
}