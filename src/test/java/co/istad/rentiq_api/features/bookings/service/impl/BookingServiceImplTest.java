package co.istad.rentiq_api.features.bookings.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.bookings.dto.request.CreateBookingRequest;
import co.istad.rentiq_api.features.bookings.dto.request.UpdateBookingStatusRequest;
import co.istad.rentiq_api.features.bookings.dto.response.BookingResponse;
import co.istad.rentiq_api.features.bookings.dto.response.PageResponse;
import co.istad.rentiq_api.features.bookings.entity.Booking;
import co.istad.rentiq_api.features.bookings.enums.BookingStatus;
import co.istad.rentiq_api.features.bookings.enums.PaymentStatus;
import co.istad.rentiq_api.features.bookings.exception.BookingAccessDeniedException;
import co.istad.rentiq_api.features.bookings.exception.InvalidBookingOperationException;
import co.istad.rentiq_api.features.bookings.mapper.BookingMapper;
import co.istad.rentiq_api.features.bookings.mapper.BookingStatusHistoryMapper;
import co.istad.rentiq_api.features.bookings.repository.BookingQrCodeRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingRepository;
import co.istad.rentiq_api.features.bookings.repository.BookingStatusHistoryRepository;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.itemrequest.repository.OfferRepository;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.service.PlatformSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final String OWNER_ID = "vendor-1";
    private static final String CUSTOMER_ID = "customer-1";
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusHistoryRepository historyRepository;
    @Mock private BookingQrCodeRepository qrCodeRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BookingMapper mapper;
    @Mock private BookingStatusHistoryMapper historyMapper;
    @Mock private QrCodeGenerator qrCodeGenerator;
    @Mock private BookingDocumentGenerator documentGenerator;
    @Mock private AdminAuditService adminAuditService;
    @Mock private PlatformSettingService platformSettingService;

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(
                bookingRepository, historyRepository, qrCodeRepository, itemRepository,
                offerRepository, categoryRepository, mapper, historyMapper,
                qrCodeGenerator, documentGenerator, adminAuditService, platformSettingService);

        lenient().when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(platformSettingService.getInteger(PlatformSettingKey.BOOKING_MAX_RENTAL_DAYS)).thenReturn(30);
    }

    private Booking rentedBooking() {
        return rentedBooking(PaymentStatus.UNPAID);
    }

    private Booking rentedBooking(PaymentStatus paymentStatus) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingRef("BK-TEST0001")
                .customerId(CUSTOMER_ID)
                .ownerId(OWNER_ID)
                .rentalStart(LocalDate.now().plusDays(1))
                .rentalEnd(LocalDate.now().plusDays(3))
                .rentalDays((short) 2)
                .bookedPricePerDay(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .securityDeposit(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("0.10"))
                .commissionAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("100.00"))
                .status(BookingStatus.RENTED)
                .paymentStatus(paymentStatus)
                .build();
    }

    // ---------------------------------------------------------------
    // Payment state truthfulness (rental payment is P2P — Rentiq never collects it)
    // ---------------------------------------------------------------

    @Test
    void create_setsPaymentStatusUnpaid_becauseRentiqNeverCollectsRentalPayment() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
                .id(itemId)
                .ownerId(OWNER_ID)
                .categoryId(CATEGORY_ID)
                .pricePerDay(new BigDecimal("50.00"))
                .depositAmount(BigDecimal.ZERO)
                .available(true)
                .approvalStatus(ItemApprovalStatus.APPROVED)
                .status(ItemStatus.ACTIVE)
                .build();

        Category category = new Category();
        category.setCommissionRate(new BigDecimal("0.10"));

        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        CreateBookingRequest request = new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        service.create(request, CUSTOMER_ID);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void create_allowsRentalAtConfiguredMaximum() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(availableItem(itemId)));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        service.create(new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(31)), CUSTOMER_ID);

        verify(bookingRepository).save(any());
    }

    @Test
    void create_rejectsRentalAboveConfiguredMaximum() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(availableItem(itemId)));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(32)), CUSTOMER_ID))
                .isInstanceOf(InvalidBookingOperationException.class).hasMessageContaining("30 days");
        verify(bookingRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Booking creation concurrency (backend audit CONC-001) — the Item row must be locked
    // (SELECT ... FOR UPDATE) before the overlap check, the same pattern already used for
    // Promotion purchases, so two concurrent requests for the same item can't both pass the
    // overlap check before either commits.
    // ---------------------------------------------------------------

    private Item availableItem(UUID itemId) {
        return Item.builder()
                .id(itemId)
                .ownerId(OWNER_ID)
                .categoryId(CATEGORY_ID)
                .pricePerDay(new BigDecimal("50.00"))
                .depositAmount(BigDecimal.ZERO)
                .available(true)
                .approvalStatus(ItemApprovalStatus.APPROVED)
                .status(ItemStatus.ACTIVE)
                .build();
    }

    @Test
    void create_locksItemRowViaFindByIdForUpdate_beforeCheckingOverlap() {
        UUID itemId = UUID.randomUUID();
        Item item = availableItem(itemId);
        Category category = new Category();
        category.setCommissionRate(new BigDecimal("0.10"));

        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        CreateBookingRequest request = new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        service.create(request, CUSTOMER_ID);

        verify(itemRepository).findByIdForUpdate(itemId);
        verify(itemRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void create_rejectsOverlappingBooking_afterLockingItem() {
        UUID itemId = UUID.randomUUID();
        Item item = availableItem(itemId);

        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any())).thenReturn(true);

        CreateBookingRequest request = new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> service.create(request, CUSTOMER_ID))
                .isInstanceOf(InvalidBookingOperationException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_rejectsDeletedItem_evenThoughFindByIdForUpdateDoesNotFilterDeleted() {
        UUID itemId = UUID.randomUUID();
        Item item = availableItem(itemId);
        item.setDeleted(true);

        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));

        CreateBookingRequest request = new CreateBookingRequest(
                itemId, null, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> service.create(request, CUSTOMER_ID))
                .isInstanceOf(ItemNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Booking completion never moves money — rental payment is P2P, outside Rentiq
    // ---------------------------------------------------------------

    @Test
    void completingRentedBooking_succeeds_andDoesNotChangePaymentStatus() {
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, "Rental finished"),
                OWNER_ID, false);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        // Rentiq never held or released this money — paymentStatus is untouched by completion.
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void completingRentedBooking_setsSecurityDepositReturnedAt() {
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                OWNER_ID, false);

        assertThat(booking.getSecurityDepositReturnedAt()).isNotNull();
    }

    @Test
    void completingRentedBooking_neverLocksTheBookingRowForRelease() {
        // BookingRepository.findByIdForUpdate existed solely to serialize escrow release,
        // which no longer happens — completion must not call it at all.
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                OWNER_ID, false);

        verify(bookingRepository, never()).findByIdForUpdate(any());
    }

    // ---------------------------------------------------------------
    // Admin audit: only admin-triggered status changes are audited
    // ---------------------------------------------------------------

    @Test
    void updateStatus_adminCompletingBooking_recordsBookingStatusChangedAudit() {
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, "Admin override"),
                "admin-1", true);

        verify(adminAuditService).record(
                AdminAuditAction.BOOKING_STATUS_CHANGED,
                AdminAuditTargetType.BOOKING,
                booking.getId().toString(),
                Map.of("status", "RENTED"),
                Map.of("status", "COMPLETED"),
                "Admin override");
    }

    @Test
    void updateStatus_ownerCompletingBooking_doesNotRecordAdminAudit() {
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                OWNER_ID, false);

        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Invalid booking status rejected
    // ---------------------------------------------------------------

    @Test
    void updateStatus_customerCannotCompleteBooking() {
        Booking booking = rentedBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                CUSTOMER_ID, false))
                .isInstanceOf(BookingAccessDeniedException.class);
    }

    @Test
    void updateStatus_adminCannotSkipDirectlyToCompletedFromPending() {
        Booking booking = rentedBooking();
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                "admin-1", true))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void updateStatus_cancelledBookingCannotBeCompletedByAdmin() {
        Booking booking = rentedBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.updateStatus(booking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.COMPLETED, null),
                "admin-1", true))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    // ---------------------------------------------------------------
    // Booking pagination (backend audit PERF-001) — GET /bookings and
    // GET /vendors/me/bookings must be paginated and scoped to the caller.
    // ---------------------------------------------------------------

    @Test
    void findMyBookings_scopesToCustomerId_andReturnsPageResponse() {
        Booking booking = rentedBooking();
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(bookingRepository.findByCustomerId(CUSTOMER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        PageResponse<BookingResponse> response = service.findMyBookings(CUSTOMER_ID, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(bookingRepository).findByCustomerId(CUSTOMER_ID, pageable);
        verify(bookingRepository, never()).findByOwnerId(any(), any());
    }

    @Test
    void findVendorBookings_scopesToOwnerId_andReturnsPageResponse() {
        Booking booking = rentedBooking();
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(bookingRepository.findByOwnerId(OWNER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(mapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));

        PageResponse<BookingResponse> response = service.findVendorBookings(OWNER_ID, pageable);

        assertThat(response.content()).hasSize(1);
        verify(bookingRepository).findByOwnerId(OWNER_ID, pageable);
        verify(bookingRepository, never()).findByCustomerId(any(), any());
    }
}
