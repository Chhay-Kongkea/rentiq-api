package co.istad.rentiq_api.features.bookings.service.impl;


import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.bookings.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.bookings.dto.request.QrScanRequest;
import co.istad.rentiq_api.features.bookings.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.bookings.dto.response.BookingQrCodeResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingResponse;
import co.istad.rentiq_api.features.bookings.dto.response.BookingStatusHistoryResponse;
import co.istad.rentiq_api.features.bookings.dto.response.PageResponse;
import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.entity.BookingQrCode;
import co.istad.rentiq_api.features.bookings.entity.BookingStatusHistory;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.bookings.exception.BookingAccessDeniedException;
import co.istad.rentiq_api.features.bookings.exception.BookingNotFoundException;
import co.istad.rentiq_api.features.bookings.exception.BookingQrCodeException;
import co.istad.rentiq_api.features.bookings.exception.InvalidBookingOperationException;
import co.istad.rentiq_api.features.bookings.mapper.BookingMapper;
import co.istad.rentiq_api.features.bookings.mapper.BookingStatusHistoryMapper;
import co.istad.rentiq_api.features.bookings.repository.BookingQrCodeRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.bookings.service.BookingService;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.itemrequest.entity.Offer;
import co.istad.rentiq_api.features.itemrequest.enums.OfferStatus;
import co.istad.rentiq_api.features.itemrequest.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class
BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.RENTED);

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingQrCodeRepository qrCodeRepository;
    private final ItemRepository itemRepository;
    private final OfferRepository offerRepository;
    private final CategoryRepository categoryRepository;
    private final BookingMapper mapper;
    private final BookingStatusHistoryMapper historyMapper;
    private final QrCodeGenerator qrCodeGenerator;
    private final BookingDocumentGenerator documentGenerator;
    private final AdminAuditService adminAuditService;

    @Override
    public BookingResponse create(CreateBookingRequest request, String customerId) {

        // Lock the Item row FIRST — every concurrent booking-creation attempt for the same
        // item serializes here, which is what makes the overlap check below race-free (the
        // same pattern already used for Promotion purchases). Without this lock, two
        // concurrent requests could both pass the overlap check before either commits.
        Item item = resolveItemForUpdate(request);

        if (item.isDeleted()) {
            throw new ItemNotFoundException(item.getId());
        }

        if (!item.isAvailable()
                || item.getApprovalStatus() != ItemApprovalStatus.APPROVED
                || item.getStatus() != ItemStatus.ACTIVE) {
            throw new InvalidBookingOperationException("Item is not available for booking");
        }

        if (!request.rentalEnd().isAfter(request.rentalStart())) {
            throw new InvalidBookingOperationException("rentalEnd must be after rentalStart");
        }

        if (bookingRepository.existsOverlappingBooking(
                item.getId(), request.rentalStart(), request.rentalEnd(), ACTIVE_STATUSES)) {
            throw new InvalidBookingOperationException("Item is already booked for the selected dates");
        }

        short rentalDays = (short) ChronoUnit.DAYS.between(request.rentalStart(), request.rentalEnd());

        BigDecimal bookedPricePerDay = item.getPricePerDay();
        BigDecimal subtotal = bookedPricePerDay.multiply(BigDecimal.valueOf(rentalDays));
        BigDecimal securityDeposit = item.getDepositAmount() != null ? item.getDepositAmount() : BigDecimal.ZERO;

        BigDecimal commissionRate = categoryRepository.findById(item.getCategoryId())
                .map(Category::getCommissionRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal commissionAmount = subtotal.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .bookingRef(generateBookingRef())
                .customerId(customerId)
                .ownerId(item.getOwnerId())
                .item(item)
                .offerId(request.offerId())
                .rentalStart(request.rentalStart())
                .rentalEnd(request.rentalEnd())
                .rentalDays(rentalDays)
                .bookedPricePerDay(bookedPricePerDay)
                .subtotal(subtotal)
                .securityDeposit(securityDeposit)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .totalAmount(subtotal.add(securityDeposit))
                .status(BookingStatus.PENDING)
                // No payment gateway, checkout, or payment-confirmation event exists yet in
                // this codebase, so no funds are actually collected or held at creation time.
                // paymentStatus stays UNPAID until a real payment-confirmation event (e.g. a
                // checkout/payment-capture endpoint or gateway webhook) is implemented and
                // explicitly moves it to HELD_IN_ESCROW.
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Booking saved = bookingRepository.save(booking);

        historyRepository.save(BookingStatusHistory.builder()
                .booking(saved)
                .oldStatus(null)
                .newStatus(BookingStatus.PENDING)
                .changedBy(customerId)
                .reason("Booking created")
                .build());

        return mapper.toResponse(saved);
    }

    /**
     * Resolves the target item id (from the offer or request directly), then locks that Item
     * row (SELECT ... FOR UPDATE) before returning it. Callers must perform all eligibility/
     * overlap validation against the RETURNED (locked) entity, not re-fetch it unlocked.
     */
    private Item resolveItemForUpdate(CreateBookingRequest request) {
        UUID itemId;

        if (request.offerId() != null) {
            Offer offer = offerRepository.findById(request.offerId())
                    .orElseThrow(() -> new NotFoundException("Offer", request.offerId()));

            if (offer.getStatus() != OfferStatus.ACCEPTED) {
                throw new InvalidBookingOperationException("Offer must be accepted before it can be booked");
            }

            itemId = offer.getItem().getId();
        } else if (request.itemId() != null) {
            itemId = request.itemId();
        } else {
            throw new InvalidBookingOperationException("Either itemId or offerId is required");
        }

        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    private String generateBookingRef() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> findMyBookings(String customerId, Pageable pageable) {
        return PageResponse.from(bookingRepository.findByCustomerId(customerId, pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse findById(UUID bookingId, String callerId, boolean isAdmin) {
        Booking booking = getOrThrow(bookingId);
        checkAccess(booking, callerId, isAdmin);
        return mapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getStatusHistory(UUID bookingId, String callerId, boolean isAdmin) {
        Booking booking = getOrThrow(bookingId);
        checkAccess(booking, callerId, isAdmin);

        return historyRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    @Override
    public BookingResponse updateStatus(UUID bookingId, UpdateBookingStatusRequest request,
                                         String callerId, boolean isAdmin) {

        Booking booking = getOrThrow(bookingId);
        BookingStatus current = booking.getStatus();
        BookingStatus target = request.status();

        boolean isCustomer = booking.getCustomerId().equals(callerId);
        boolean isOwner = booking.getOwnerId().equals(callerId);

        if (!isAdmin && !isCustomer && !isOwner) {
            throw new BookingAccessDeniedException();
        }

        if (!isAdmin) {
            validateTransition(current, target, isCustomer, isOwner);
        } else if (current == target) {
            throw new InvalidBookingOperationException("Booking is already in status " + target);
        } else if (target == BookingStatus.COMPLETED && current != BookingStatus.RENTED) {
            // Booking-lifecycle invariant, enforced even for admin overrides: a booking can
            // only be completed from RENTED. (Rentiq never holds or moves rental money — see
            // PaymentStatus javadoc — so this is about state-machine integrity, not escrow.)
            throw new InvalidBookingOperationException("Only a RENTED booking can be completed");
        }

        transition(booking, target, callerId, request.reason());

        if (target == BookingStatus.APPROVED) {
            booking.setOwnerConfirmedAt(OffsetDateTime.now());
        }

        if (target == BookingStatus.COMPLETED) {
            // Rental payment is P2P (renter pays vendor directly, outside Rentiq), so
            // completing a booking must NOT credit any wallet or touch paymentStatus.
            booking.setSecurityDepositReturnedAt(OffsetDateTime.now());
        }

        Booking saved = bookingRepository.save(booking);

        if (isAdmin) {
            adminAuditService.record(
                    AdminAuditAction.BOOKING_STATUS_CHANGED,
                    AdminAuditTargetType.BOOKING,
                    saved.getId().toString(),
                    Map.of("status", current.name()),
                    Map.of("status", target.name()),
                    request.reason());
        }

        return mapper.toResponse(saved);
    }

    private void validateTransition(BookingStatus current, BookingStatus target,
                                     boolean isCustomer, boolean isOwner) {

        switch (current) {
            case PENDING -> {
                if (target == BookingStatus.APPROVED || target == BookingStatus.REJECTED) {
                    if (!isOwner) {
                        throw new BookingAccessDeniedException("Only the vendor can approve or reject a booking");
                    }
                } else if (target == BookingStatus.CANCELLED) {
                    if (!isCustomer) {
                        throw new BookingAccessDeniedException("Only the customer can cancel a booking");
                    }
                } else {
                    throw new InvalidBookingOperationException("Cannot transition from PENDING to " + target);
                }
            }
            case APPROVED -> {
                if (target == BookingStatus.CANCELLED) {
                    if (!isCustomer) {
                        throw new BookingAccessDeniedException("Only the customer can cancel a booking");
                    }
                } else if (target == BookingStatus.RENTED) {
                    throw new InvalidBookingOperationException(
                            "Use the pickup QR code scan to mark a booking as picked up");
                } else {
                    throw new InvalidBookingOperationException("Cannot transition from APPROVED to " + target);
                }
            }
            case RENTED -> {
                if (target == BookingStatus.COMPLETED) {
                    if (!isOwner) {
                        throw new BookingAccessDeniedException("Only the vendor can complete a booking");
                    }
                } else {
                    throw new InvalidBookingOperationException("Cannot transition from RENTED to " + target);
                }
            }
            default -> throw new InvalidBookingOperationException(
                    "Cannot change status of a booking in " + current + " state");
        }
    }

    private void transition(Booking booking, BookingStatus newStatus, String changedBy, String reason) {
        BookingStatus old = booking.getStatus();
        booking.setStatus(newStatus);

        historyRepository.save(BookingStatusHistory.builder()
                .booking(booking)
                .oldStatus(old)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> findVendorBookings(String ownerId, Pageable pageable) {
        return PageResponse.from(bookingRepository.findByOwnerId(ownerId, pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getVendorSchedule(String ownerId, LocalDate from, LocalDate to) {
        return bookingRepository.findScheduleByOwnerId(ownerId, from, to)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public BookingQrCodeResponse getOrCreateQrCode(UUID bookingId, String customerId) {
        Booking booking = getOrThrow(bookingId);

        if (!booking.getCustomerId().equals(customerId)) {
            throw new BookingAccessDeniedException();
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new InvalidBookingOperationException("QR code is only available for approved bookings");
        }

        OffsetDateTime expiresAt = booking.getRentalEnd().plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime now = OffsetDateTime.now();

        BookingQrCode qr = qrCodeRepository.findByBookingId(bookingId).orElse(null);

        if (qr == null) {
            qr = qrCodeRepository.save(BookingQrCode.builder()
                    .booking(booking)
                    .qrToken(UUID.randomUUID().toString())
                    .isValid(true)
                    .expiresAt(expiresAt)
                    .build());
        } else if (!Boolean.TRUE.equals(qr.getIsValid()) || (qr.getExpiresAt() != null && qr.getExpiresAt().isBefore(now))) {
            qr.setQrToken(UUID.randomUUID().toString());
            qr.setIsValid(true);
            qr.setScannedAt(null);
            qr.setScannedBy(null);
            qr.setExpiresAt(expiresAt);
            qr = qrCodeRepository.save(qr);
        }

        String base64Image = qrCodeGenerator.generateBase64Png(qr.getQrToken());

        return new BookingQrCodeResponse(booking.getId(), qr.getQrToken(), base64Image, qr.getExpiresAt());
    }

    @Override
    public BookingResponse scanQrCode(QrScanRequest request, String vendorId) {
        BookingQrCode qr = qrCodeRepository.findByQrTokenAndIsValidTrue(request.qrToken())
                .orElseThrow(() -> new BookingQrCodeException("Invalid or already used QR code"));

        if (qr.getExpiresAt() != null && qr.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BookingQrCodeException("QR code has expired");
        }

        Booking booking = qr.getBooking();

        if (!booking.getOwnerId().equals(vendorId)) {
            throw new BookingAccessDeniedException("This booking does not belong to you");
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new InvalidBookingOperationException("Booking must be approved before pickup can be verified");
        }

        qr.setIsValid(false);
        qr.setScannedAt(OffsetDateTime.now());
        qr.setScannedBy(vendorId);
        qrCodeRepository.save(qr);

        transition(booking, BookingStatus.RENTED, vendorId, "Verified via QR code scan");

        return mapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceipt(UUID bookingId, String callerId, boolean isAdmin) {
        Booking booking = getOrThrow(bookingId);
        checkAccess(booking, callerId, isAdmin);
        return documentGenerator.generateReceipt(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoice(UUID bookingId, String callerId, boolean isAdmin) {
        Booking booking = getOrThrow(bookingId);
        checkAccess(booking, callerId, isAdmin);
        return documentGenerator.generateInvoice(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> findAllForAdmin(int pageNumber, int pageSize, BookingStatus status) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Booking> page = status != null
                ? bookingRepository.findByStatus(status, pageable)
                : bookingRepository.findAll(pageable);

        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse findByIdForAdmin(UUID bookingId) {
        return mapper.toResponse(getOrThrow(bookingId));
    }

    private Booking getOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    private void checkAccess(Booking booking, String callerId, boolean isAdmin) {
        if (!isAdmin
                && !booking.getCustomerId().equals(callerId)
                && !booking.getOwnerId().equals(callerId)) {
            throw new BookingAccessDeniedException();
        }
    }
}
