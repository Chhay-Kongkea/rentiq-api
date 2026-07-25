package co.istad.rentiq_api.features.booking.service.impl;

import co.istad.rentiq_api.features.booking.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.booking.dto.request.ScanQrRequest;
import co.istad.rentiq_api.features.booking.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.booking.dto.response.*;
import co.istad.rentiq_api.features.booking.entity.Booking;
import co.istad.rentiq_api.features.booking.entity.BookingQrCode;
import co.istad.rentiq_api.features.booking.entity.BookingStatusHistory;
import co.istad.rentiq_api.features.booking.exception.*;
import co.istad.rentiq_api.features.booking.mapper.BookingMapper;
import co.istad.rentiq_api.features.booking.repository.BookingQrCodeRepository;
import co.istad.rentiq_api.features.booking.repository.BookingRepository;
import co.istad.rentiq_api.features.booking.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.booking.repository.ItemAvailabilityRepository;
import co.istad.rentiq_api.features.booking.service.BookingService;
import co.istad.rentiq_api.features.booking.service.BookingStatusTransitionValidator;
import co.istad.rentiq_api.features.booking.service.BookingStatusTransitionValidator.Actor;

import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.item.entity.Item; // adjust package
import co.istad.rentiq_api.features.item.repository.ItemRepository; // adjust package
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "APPROVED", "RENTED");

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingQrCodeRepository qrCodeRepository;
//    private final ItemAvailabilityRepository itemAvailabilityRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusTransitionValidator transitionValidator;

    @Override
    @Transactional
    public BookingResponse createBooking(
            String customerId,
            CreateBookingRequest request
    ) {


        if (!request.rentalEnd().isAfter(request.rentalStart())) {
            throw new InvalidBookingDateRangeException(
                    "rentalEnd must be after rentalStart"
            );
        }


        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() ->
                        new BookingItemNotFoundException(request.itemId())
                );


        if (item.getOwnerId().equals(customerId)) {
            throw new BookingAccessDeniedException(
                    "You cannot book your own item"
            );
        }


        if (!item.isAvailable()
                || Boolean.FALSE.equals(item.getApprovalStatus())) {

            throw new ItemNotAvailableException(item.getId());
        }



        // CHECK DATE OVERLAP
        boolean overlap =
                bookingRepository.existsOverlappingBooking(
                        item.getId(),
                        request.rentalStart(),
                        request.rentalEnd()
                );


        if (overlap) {
            throw new ItemNotAvailableException(item.getId());
        }



        short rentalDays =
                (short)
                        (ChronoUnit.DAYS.between(
                                request.rentalStart(),
                                request.rentalEnd()
                        ) + 1);



        BigDecimal subtotal =
                item.getPricePerDay()
                        .multiply(BigDecimal.valueOf(rentalDays));


        BigDecimal deposit =
                item.getDepositAmount() != null
                        ? item.getDepositAmount()
                        : BigDecimal.ZERO;



        BigDecimal commissionRate = BigDecimal.ZERO;


        Category category =
                categoryRepository.findById(
                        Integer.valueOf(item.getCategoryId())
                ).orElse(null);



        if(category != null
                && category.getCommissionRate()!=null){

            commissionRate =
                    BigDecimal.valueOf(
                            category.getCommissionRate()
                    );
        }



        BigDecimal commissionAmount =
                subtotal
                        .multiply(commissionRate)
                        .setScale(2, RoundingMode.HALF_UP);



        BigDecimal totalAmount =
                subtotal.add(deposit);



        Instant now = Instant.now();



        Booking booking = Booking.builder()

                .bookingRef(generateBookingRef())

                .customerId(customerId)

                .ownerId(item.getOwnerId())

                .itemId(item.getId())

                .offerId(request.offerId())

                .rentalStart(request.rentalStart())

                .rentalEnd(request.rentalEnd())

                .rentalDays(rentalDays)

                .bookedPricePerDay(item.getPricePerDay())

                .subtotal(subtotal)

                .securityDeposit(deposit)

                .commissionRate(commissionRate)

                .commissionAmount(commissionAmount)

                .totalAmount(totalAmount)

                .status("PENDING")

                .paymentStatus("UNPAID")

                .createdAt(now)

                .updatedAt(now)

                .build();



        bookingRepository.save(booking);



        recordHistory(
                booking.getId(),
                null,
                "PENDING",
                customerId,
                "Booking created"
        );


        return bookingMapper.toResponse(booking);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listMyBookings(String customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable).map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(String userId, UUID bookingId) {
        Booking booking = requireParticipant(userId, bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getStatusHistory(String userId, UUID bookingId) {
        requireParticipant(userId, bookingId);
        return historyRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(bookingMapper::toHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse updateStatus(String userId, UUID bookingId, UpdateBookingStatusRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        Actor actor = resolveActor(userId, booking);
        String newStatus = request.status().toUpperCase();

        if (!transitionValidator.isAllowed(actor, booking.getStatus(), newStatus)) {
            throw new InvalidBookingStatusTransitionException(booking.getStatus(), newStatus);
        }

        applyTransitionSideEffects(booking, newStatus);

        String oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        recordHistory(booking.getId(), oldStatus, newStatus, userId, request.reason());
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listVendorBookings(String ownerId, Pageable pageable) {
        return bookingRepository.findByOwnerId(ownerId, pageable).map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getVendorSchedule(String ownerId, LocalDate from, LocalDate to) {
        return bookingRepository.findScheduleForOwner(ownerId, from, to, ACTIVE_STATUSES).stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public QrCodeResponse getOrCreateQrCode(String userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getCustomerId().equals(userId)) {
            throw new BookingAccessDeniedException("Only the customer can generate a pickup QR code");
        }
        if (!"APPROVED".equals(booking.getStatus())) {
            throw new InvalidQrCodeException("QR code is only available once the booking is APPROVED");
        }

        BookingQrCode qr = qrCodeRepository.findByBookingId(bookingId)
                .filter(existing -> Boolean.TRUE.equals(existing.getIsValid()))
                .orElseGet(() -> {
                    BookingQrCode created = BookingQrCode.builder()
                            .bookingId(bookingId)
                            .qrToken(UUID.randomUUID().toString())
                            .isValid(true)
                            .expiresAt(booking.getRentalStart().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                            .build();
                    return qrCodeRepository.save(created);
                });

        return new QrCodeResponse(qr.getQrToken(), qr.getExpiresAt());
    }

    @Override
    @Transactional
    public ScanResultResponse scanQrCode(String vendorId, ScanQrRequest request) {
        BookingQrCode qr = qrCodeRepository.findByQrTokenAndIsValidTrue(request.qrToken())
                .orElseThrow(() -> new InvalidQrCodeException("QR code is invalid or already used"));

        if (qr.getExpiresAt() != null && qr.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidQrCodeException("QR code has expired");
        }

        Booking booking = bookingRepository.findById(qr.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(qr.getBookingId()));

        if (!booking.getOwnerId().equals(vendorId)) {
            throw new BookingAccessDeniedException("Only the item owner can scan this QR code");
        }
        if (!transitionValidator.isAllowed(Actor.OWNER, booking.getStatus(), "RENTED")) {
            throw new InvalidBookingStatusTransitionException(booking.getStatus(), "RENTED");
        }

        Instant now = Instant.now();
        qr.setIsValid(false);
        qr.setScannedAt(now);
        qr.setScannedBy(vendorId);
        qrCodeRepository.save(qr);

        String oldStatus = booking.getStatus();
        booking.setStatus("RENTED");
        booking.setUpdatedAt(now);
        bookingRepository.save(booking);
        recordHistory(booking.getId(), oldStatus, "RENTED", vendorId, "Verified via QR scan");

        return new ScanResultResponse(booking.getId(), booking.getBookingRef(), booking.getStatus(), now);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(String userId, UUID bookingId) {
        Booking booking = requireParticipant(userId, bookingId);
        return bookingMapper.toReceipt(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String userId, UUID bookingId) {
        Booking booking = requireParticipant(userId, bookingId);
        return bookingMapper.toInvoice(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> adminListBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse adminGetBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return bookingMapper.toResponse(booking);
    }

    // ---------- helpers ----------

    private Booking requireParticipant(String userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getCustomerId().equals(userId) && !booking.getOwnerId().equals(userId)) {
            throw new BookingAccessDeniedException("You do not have access to this booking");
        }
        return booking;
    }

    private Actor resolveActor(String userId, Booking booking) {
        if (booking.getCustomerId().equals(userId)) return Actor.CUSTOMER;
        if (booking.getOwnerId().equals(userId)) return Actor.OWNER;
        if (isAdmin()) return Actor.ADMIN;
        throw new BookingAccessDeniedException("You do not have access to this booking");
    }

    // ASSUMPTION: admin realm role surfaces as authority "ROLE_ADMIN" via Spring Security.
    // Adjust to match your SecurityConfig / Keycloak role mapping if different.
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void applyTransitionSideEffects(
            Booking booking,
            String status
    ){

        switch(status){

            case "APPROVED" ->
                    booking.setOwnerConfirmedAt(
                            Instant.now()
                    );


            case "COMPLETED" ->
                    booking.setSecurityDepositReturnedAt(
                            Instant.now()
                    );


            default -> {
            }
        }
    }


    private void recordHistory(UUID bookingId, String oldStatus, String newStatus, String changedBy, String reason) {
        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(bookingId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .createdAt(Instant.now())
                .build());
    }

    private String generateBookingRef() {
        String ref;
        do {
            ref = "RIQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingRef(ref));
        return ref;
    }
}