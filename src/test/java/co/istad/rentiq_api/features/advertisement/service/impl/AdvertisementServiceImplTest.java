package co.istad.rentiq_api.features.advertisement.service.impl;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.advertisement.dto.request.CreateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.RejectAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.dto.request.UpdateAdvertisementRequest;
import co.istad.rentiq_api.features.advertisement.entity.Advertisement;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementPackage;
import co.istad.rentiq_api.features.advertisement.enums.AdvertisementStatus;
import co.istad.rentiq_api.features.advertisement.mapper.AdvertisementMapper;
import co.istad.rentiq_api.features.advertisement.repository.AdvertisementRepository;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
import co.istad.rentiq_api.features.wallet.dto.response.WalletResponse;
import co.istad.rentiq_api.features.wallet.exception.WalletException;
import co.istad.rentiq_api.features.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceImplTest {

    private static final String VENDOR_ID = "vendor-1";
    private static final String OTHER_VENDOR_ID = "vendor-2";

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private AdvertisementMapper advertisementMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;
    @Mock private WalletService walletService;
    @Mock private PlatformPricingService platformPricingService;

    private AdvertisementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdvertisementServiceImpl(
                advertisementRepository, itemRepository, advertisementMapper, adminAuditService,
                notificationService, walletService, platformPricingService);
        lenient().when(advertisementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Item eligibleItem(UUID id, String ownerId) {
        return Item.builder()
                .id(id).ownerId(ownerId)
                .approvalStatus(ItemApprovalStatus.APPROVED).status(ItemStatus.ACTIVE)
                .build();
    }

    private Advertisement pendingAd(UUID itemId, String vendorId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return Advertisement.builder()
                .id(UUID.randomUUID())
                .vendorId(vendorId)
                .itemId(itemId)
                .title("Featured Camera")
                .packageType(AdvertisementPackage.AD_7_DAYS)
                .durationDays(7)
                .quotedPrice(new BigDecimal("6.00"))
                .quotedCurrency("USD")
                .quotedAt(OffsetDateTime.now())
                .status(AdvertisementStatus.PENDING)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    private WalletResponse walletResponse(String currency, BigDecimal balance) {
        return WalletResponse.builder().id(UUID.randomUUID()).ownerId(VENDOR_ID).balance(balance).currency(currency).build();
    }

    // ---------------------------------------------------------------
    // Vendor: create — quote generation
    // ---------------------------------------------------------------

    @Test
    void create_vendorCreatesAd3Days_derivesDurationAndEndAt_generatesQuote_noWalletCharge() {
        UUID itemId = UUID.randomUUID();
        Item item = eligibleItem(itemId, VENDOR_ID);
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("50.00")));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_3_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("3.00")));

        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_3_DAYS, "Featured Camera", "desc", null, start);

        service.create(request, VENDOR_ID);

        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementRepository).save(captor.capture());
        Advertisement saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        assertThat(saved.getVendorId()).isEqualTo(VENDOR_ID);
        assertThat(saved.getItemId()).isEqualTo(itemId);
        assertThat(saved.getPackageType()).isEqualTo(AdvertisementPackage.AD_3_DAYS);
        assertThat(saved.getDurationDays()).isEqualTo(3);
        assertThat(saved.getEndAt()).isEqualTo(start.plusDays(3));
        assertThat(saved.getQuotedPrice()).isEqualByComparingTo("3.00");
        assertThat(saved.getQuotedCurrency()).isEqualTo("USD");
        assertThat(saved.getQuotedAt()).isNotNull();
        assertThat(saved.getPrice()).isNull();
        assertThat(saved.getCurrency()).isNull();

        verify(walletService, never()).chargeAdvertisement(any(), any(), any(), any());
    }

    @Test
    void create_khrWallet_quotesInKhr() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("KHR", new BigDecimal("50000")));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_7_DAYS, "KHR"))
                .thenReturn(Optional.of(new BigDecimal("24000")));

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        service.create(request, VENDOR_ID);

        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementRepository).save(captor.capture());
        assertThat(captor.getValue().getQuotedPrice()).isEqualByComparingTo("24000");
        assertThat(captor.getValue().getQuotedCurrency()).isEqualTo("KHR");
        verify(walletService, never()).chargeAdvertisement(any(), any(), any(), any());
    }

    @Test
    void create_doesNotRejectForInsufficientBalance_onlyPricingMustResolve() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        // Balance is far below the package price — must not matter at submission time.
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", BigDecimal.ZERO));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_7_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("6.00")));

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        service.create(request, VENDOR_ID);

        verify(advertisementRepository).save(any());
        verify(walletService, never()).chargeAdvertisement(any(), any(), any(), any());
    }

    @Test
    void create_rejectsUnsupportedWalletCurrency_forQuoting() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("EUR", BigDecimal.TEN));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_7_DAYS, "EUR"))
                .thenReturn(Optional.empty());

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(request, VENDOR_ID)).isInstanceOf(InvalidOperationException.class);
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void create_rejectsAnotherVendorsItem() {
        UUID itemId = UUID.randomUUID();
        Item item = eligibleItem(itemId, OTHER_VENDOR_ID);
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(request, VENDOR_ID)).isInstanceOf(ForbiddenException.class);
        verify(advertisementRepository, never()).save(any());
        verifyNoInteractions(walletService);
    }

    @Test
    void create_rejectsIneligibleItem_whenNotApprovedOrActive() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).ownerId(VENDOR_ID)
                .approvalStatus(ItemApprovalStatus.PENDING).status(ItemStatus.ACTIVE).build();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(item));

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(request, VENDOR_ID)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void create_rejectsUnknownItem() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.empty());

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(request, VENDOR_ID)).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void create_rejectsStartAtTooFarInFuture() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdAndDeletedFalse(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));

        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                itemId, AdvertisementPackage.AD_7_DAYS, "Ad", null, null, OffsetDateTime.now().plusDays(45));

        assertThatThrownBy(() -> service.create(request, VENDOR_ID)).isInstanceOf(InvalidOperationException.class);
        verify(advertisementRepository, never()).save(any());
        verifyNoInteractions(walletService);
    }

    // ---------------------------------------------------------------
    // Vendor: update / cancel
    // ---------------------------------------------------------------

    @Test
    void update_rejectsEditingAnotherVendorsAd() {
        Advertisement ad = pendingAd(UUID.randomUUID(), OTHER_VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                AdvertisementPackage.AD_3_DAYS, "New title", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.update(ad.getId(), request, VENDOR_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_rejectsEditingApprovedAdvertisement() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        ad.setStatus(AdvertisementStatus.APPROVED);
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                AdvertisementPackage.AD_3_DAYS, "New title", null, null, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.update(ad.getId(), request, VENDOR_ID)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void update_rejectedAdvertisement_resubmitsAsPending_recalculatesPackage_andReQuotesAtCurrentPrice() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        ad.setStatus(AdvertisementStatus.REJECTED);
        ad.setRejectionReason("Bad photo");
        ad.setReviewedBy("admin-1");
        ad.setReviewedAt(OffsetDateTime.now());
        // Old quote was 6 USD (AD_7_DAYS) — resubmission must replace it with the CURRENT price.
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("50.00")));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_14_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("8.00")));

        OffsetDateTime newStart = OffsetDateTime.now().plusDays(2);
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                AdvertisementPackage.AD_14_DAYS, "Updated title", "desc", null, newStart);

        service.update(ad.getId(), request, VENDOR_ID);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        assertThat(ad.getRejectionReason()).isNull();
        assertThat(ad.getReviewedBy()).isNull();
        assertThat(ad.getReviewedAt()).isNull();
        assertThat(ad.getTitle()).isEqualTo("Updated title");
        assertThat(ad.getPackageType()).isEqualTo(AdvertisementPackage.AD_14_DAYS);
        assertThat(ad.getDurationDays()).isEqualTo(14);
        assertThat(ad.getEndAt()).isEqualTo(newStart.plusDays(14));
        assertThat(ad.getQuotedPrice()).isEqualByComparingTo("8.00");
        assertThat(ad.getQuotedCurrency()).isEqualTo("USD");
        assertThat(ad.getPrice()).isNull();
        assertThat(ad.getCurrency()).isNull();
        verify(walletService, never()).chargeAdvertisement(any(), any(), any(), any());
    }

    @Test
    void update_pendingContentOnlyEdit_preservesExistingQuote_noRequote() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        // Original quote frozen at creation: 6.00 USD for AD_7_DAYS.
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        OffsetDateTime newStart = OffsetDateTime.now().plusDays(3);
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                AdvertisementPackage.AD_7_DAYS, "New title only", "new desc", null, newStart);

        service.update(ad.getId(), request, VENDOR_ID);

        assertThat(ad.getTitle()).isEqualTo("New title only");
        assertThat(ad.getStartAt()).isEqualTo(newStart);
        assertThat(ad.getQuotedPrice()).isEqualByComparingTo("6.00");
        assertThat(ad.getQuotedCurrency()).isEqualTo("USD");
        verifyNoInteractions(walletService);
        verifyNoInteractions(platformPricingService);
    }

    @Test
    void update_pendingPackageChange_requotesAtCurrentPrice() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        // Original quote: AD_7_DAYS at 6.00 USD.
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("50.00")));
        when(platformPricingService.getAdvertisementPrice(AdvertisementPackage.AD_3_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("3.00")));

        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                AdvertisementPackage.AD_3_DAYS, "Featured Camera", null, null, OffsetDateTime.now().plusDays(1));

        service.update(ad.getId(), request, VENDOR_ID);

        assertThat(ad.getPackageType()).isEqualTo(AdvertisementPackage.AD_3_DAYS);
        assertThat(ad.getQuotedPrice()).isEqualByComparingTo("3.00");
        assertThat(ad.getQuotedCurrency()).isEqualTo("USD");
    }

    @Test
    void cancel_pendingAdvertisement_becomesCancelled_noWalletInteraction() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        service.cancel(ad.getId(), VENDOR_ID);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.CANCELLED);
        verifyNoInteractions(walletService);
    }

    @Test
    void cancel_approvedAndAlreadyChargedAdvertisement_givesNoRefund() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        ad.setStatus(AdvertisementStatus.APPROVED);
        ad.setPrice(new BigDecimal("6.00"));
        ad.setCurrency("USD");
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        service.cancel(ad.getId(), VENDOR_ID);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.CANCELLED);
        assertThat(ad.getPrice()).isEqualByComparingTo("6.00");
        verifyNoInteractions(walletService);
    }

    @Test
    void cancel_rejectsAlreadyExpiredAdvertisement() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().minusDays(10), OffsetDateTime.now().minusDays(1));
        ad.setStatus(AdvertisementStatus.EXPIRED);
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> service.cancel(ad.getId(), VENDOR_ID)).isInstanceOf(InvalidStateException.class);
    }

    // ---------------------------------------------------------------
    // Admin: approve — charges the FROZEN quote, never re-resolves current pricing
    // ---------------------------------------------------------------

    private void stubEligibleItemForApproval(Advertisement ad) {
        when(itemRepository.findByIdAndDeletedFalse(ad.getItemId())).thenReturn(
                Optional.of(eligibleItem(ad.getItemId(), ad.getVendorId())));
    }

    private Advertisement quotedAd(AdvertisementPackage packageType, BigDecimal quotedPrice, String quotedCurrency,
                                    OffsetDateTime startAt, OffsetDateTime endAt) {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, startAt, endAt);
        ad.setPackageType(packageType);
        ad.setQuotedPrice(quotedPrice);
        ad.setQuotedCurrency(quotedCurrency);
        return ad;
    }

    @Test
    void adminApprove_usdWallet_ad3Days_chargesQuotedAmount_andFreezesPrice() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_3_DAYS, new BigDecimal("3.00"), "USD",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("3.00"), "USD");
        assertThat(ad.getPrice()).isEqualByComparingTo("3.00");
        assertThat(ad.getCurrency()).isEqualTo("USD");
        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.ACTIVE);
        verifyNoInteractions(platformPricingService);
    }

    @Test
    void adminApprove_khrWallet_ad3Days_chargesQuotedAmount() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_3_DAYS, new BigDecimal("12000"), "KHR",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("KHR", new BigDecimal("50000")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("12000"), "KHR");
        assertThat(ad.getPrice()).isEqualByComparingTo("12000");
        assertThat(ad.getCurrency()).isEqualTo("KHR");
    }

    @Test
    void adminApprove_usdWallet_ad7Days_chargesQuotedAmount() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("6.00"), "USD");
    }

    @Test
    void adminApprove_khrWallet_ad7Days_chargesQuotedAmount() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("24000"), "KHR",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("KHR", new BigDecimal("50000")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("24000"), "KHR");
    }

    @Test
    void adminApprove_usdWallet_ad14Days_chargesQuotedAmount() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_14_DAYS, new BigDecimal("10.00"), "USD",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("10.00"), "USD");
    }

    @Test
    void adminApprove_khrWallet_ad14Days_chargesQuotedAmount() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_14_DAYS, new BigDecimal("40000"), "KHR",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("KHR", new BigDecimal("50000")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("40000"), "KHR");
    }

    @Test
    void adminApprove_priceChangedAfterSubmission_stillChargesFrozenQuote_notCurrentPrice() {
        // Submitted when AD_7_DAYS USD was 6.00 — Admin later raised it to 8.00, but this
        // advertisement's quote was frozen at submission time and must not be re-resolved.
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        verify(walletService).chargeAdvertisement(ad.getId(), VENDOR_ID, new BigDecimal("6.00"), "USD");
        assertThat(ad.getPrice()).isEqualByComparingTo("6.00");
        // PlatformPricingService is never consulted during approval — only the stored quote is used.
        verifyNoInteractions(platformPricingService);
    }

    @Test
    void adminApprove_withinWindow_becomesActive_chargesWallet_andAudits() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.ACTIVE);
        assertThat(ad.getReviewedBy()).isEqualTo("admin-1");
        assertThat(ad.getReviewedAt()).isNotNull();
        verify(adminAuditService).record(
                eq(AdminAuditAction.ADVERTISEMENT_APPROVED), eq(AdminAuditTargetType.ADVERTISEMENT), eq(ad.getId().toString()),
                eq(Map.of("status", "PENDING")), any(), eq(null));
        verify(notificationService).notifyUser(
                eq(VENDOR_ID), eq(NotificationType.ADVERTISEMENT), any(), any(),
                eq(NotificationReferenceType.ADVERTISEMENT), eq(ad.getId()));
    }

    @Test
    void adminApprove_futureStart_becomesApproved_andChargesWallet() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().plusDays(3), OffsetDateTime.now().plusDays(10));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("20.00")));

        service.adminApprove(ad.getId(), "admin-1");

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.APPROVED);
        verify(walletService).chargeAdvertisement(any(), any(), any(), any());
    }

    @Test
    void adminApprove_rejectsAlreadyRejectedAdvertisement_andDoesNotChargeWallet() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        ad.setStatus(AdvertisementStatus.REJECTED);
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(InvalidStateException.class);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(walletService);
    }

    @Test
    void adminApprove_rejectsWindowThatHasAlreadyEnded_noChargeAttempted() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID,
                OffsetDateTime.now().minusDays(10), OffsetDateTime.now().minusDays(1));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(InvalidOperationException.class);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(walletService);
    }

    @Test
    void adminApprove_itemNoLongerEligible_leavesPending_noChargeNoAuditNoNotification() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        when(itemRepository.findByIdAndDeletedFalse(ad.getItemId())).thenReturn(Optional.of(
                Item.builder().id(ad.getItemId()).ownerId(VENDOR_ID)
                        .approvalStatus(ItemApprovalStatus.PENDING).status(ItemStatus.ACTIVE).build()));

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(InvalidOperationException.class);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(walletService);
    }

    @Test
    void adminApprove_missingQuote_rejected_noCharge() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        ad.setQuotedPrice(null);
        ad.setQuotedCurrency(null);
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(InvalidOperationException.class);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        verifyNoInteractions(walletService);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminApprove_insufficientBalance_leavesPending_noAuditNoNotification() {
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("USD", new BigDecimal("1.00")));
        org.mockito.Mockito.doThrow(new WalletException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance"))
                .when(walletService).chargeAdvertisement(any(), any(), any(), any());

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(WalletException.class);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        assertThat(ad.getPrice()).isNull();
        assertThat(ad.getRejectionReason()).isNull();
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminApprove_walletCurrencyNoLongerMatchesQuote_leavesPending_noCharge() {
        // Quoted in USD at submission time; wallet currency has since become something else —
        // approval must fail safely rather than convert or silently re-quote.
        Advertisement ad = quotedAd(AdvertisementPackage.AD_7_DAYS, new BigDecimal("6.00"), "USD",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));
        stubEligibleItemForApproval(ad);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(walletResponse("KHR", new BigDecimal("100000")));

        assertThatThrownBy(() -> service.adminApprove(ad.getId(), "admin-1")).isInstanceOf(InvalidOperationException.class);

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.PENDING);
        verify(walletService, never()).chargeAdvertisement(any(), any(), any(), any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Admin: reject
    // ---------------------------------------------------------------

    @Test
    void adminReject_storesReasonAndReviewer_andAudits_noWalletInteraction() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));

        service.adminReject(ad.getId(), new RejectAdvertisementRequest("Misleading image"), "admin-1");

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.REJECTED);
        assertThat(ad.getRejectionReason()).isEqualTo("Misleading image");
        assertThat(ad.getReviewedBy()).isEqualTo("admin-1");
        assertThat(ad.getPrice()).isNull();
        verify(adminAuditService).record(
                AdminAuditAction.ADVERTISEMENT_REJECTED, AdminAuditTargetType.ADVERTISEMENT, ad.getId().toString(),
                Map.of("status", "PENDING"), Map.of("status", "REJECTED"), "Misleading image");
        verify(notificationService).notifyUser(
                eq(VENDOR_ID), eq(NotificationType.ADVERTISEMENT), any(), any(),
                eq(NotificationReferenceType.ADVERTISEMENT), eq(ad.getId()));
        verifyNoInteractions(walletService);
    }

    @Test
    void rejectRequest_blankReason_failsBeanValidation() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new RejectAdvertisementRequest(" "));
            assertThat(violations).isNotEmpty();
        }
    }

    // ---------------------------------------------------------------
    // Admin: expire
    // ---------------------------------------------------------------

    @Test
    void adminExpire_activeAdvertisement_becomesExpired_andAudits() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().minusDays(5), OffsetDateTime.now().minusHours(1));
        ad.setStatus(AdvertisementStatus.ACTIVE);
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));

        service.adminExpire(ad.getId(), "admin-1");

        assertThat(ad.getStatus()).isEqualTo(AdvertisementStatus.EXPIRED);
        verify(adminAuditService).record(
                AdminAuditAction.ADVERTISEMENT_EXPIRED, AdminAuditTargetType.ADVERTISEMENT, ad.getId().toString(),
                Map.of("status", "ACTIVE"), Map.of("status", "EXPIRED"), null);
        // Expiration is a routine lifecycle transition, not a moderation action — no notification (see final report).
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(walletService);
    }

    @Test
    void adminExpire_rejectsPendingAdvertisement() {
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(8));
        when(advertisementRepository.findByIdForUpdate(ad.getId())).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> service.adminExpire(ad.getId(), "admin-1")).isInstanceOf(InvalidStateException.class);
    }

    // ---------------------------------------------------------------
    // Public discovery
    // ---------------------------------------------------------------

    @Test
    void getPublicAdvertisement_returnsMappedResponse_whenPubliclyVisible() {
        UUID id = UUID.randomUUID();
        Advertisement ad = pendingAd(UUID.randomUUID(), VENDOR_ID, OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(1));
        ad.setStatus(AdvertisementStatus.ACTIVE);
        when(advertisementRepository.findPubliclyVisibleById(org.mockito.ArgumentMatchers.eq(id), any(), any()))
                .thenReturn(Optional.of(ad));
        when(advertisementMapper.toPublicResponse(ad)).thenReturn(
                new co.istad.rentiq_api.features.advertisement.dto.response.PublicAdvertisementResponse(
                        id, ad.getItemId(), ad.getTitle(), null, null, ad.getStartAt(), ad.getEndAt()));

        var response = service.getPublicAdvertisement(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void getPublicAdvertisement_notFound_whenNotPubliclyVisible() {
        UUID id = UUID.randomUUID();
        when(advertisementRepository.findPubliclyVisibleById(org.mockito.ArgumentMatchers.eq(id), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicAdvertisement(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicAdvertisements_queriesOnlyApprovedAndActiveStatuses() {
        Pageable pageable = mock(Pageable.class);
        when(advertisementRepository.findPubliclyVisible(any(), any(), any(), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.getPublicAdvertisements(null, pageable);

        ArgumentCaptor<java.util.List<AdvertisementStatus>> statusesCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(advertisementRepository).findPubliclyVisible(statusesCaptor.capture(), any(), any(), org.mockito.ArgumentMatchers.eq(pageable));
        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(AdvertisementStatus.APPROVED, AdvertisementStatus.ACTIVE);
    }
}
