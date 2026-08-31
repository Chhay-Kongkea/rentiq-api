package co.istad.rentiq_api.features.promotion.service.impl;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.InvalidStateException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
import co.istad.rentiq_api.features.item.entity.Item;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.enums.ItemStatus;
import co.istad.rentiq_api.features.item.exception.ItemNotFoundException;
import co.istad.rentiq_api.features.item.repository.ItemRepository;
import co.istad.rentiq_api.features.promotion.dto.request.CreatePromotionRequest;
import co.istad.rentiq_api.features.promotion.dto.request.SuspendPromotionRequest;
import co.istad.rentiq_api.features.promotion.entity.Promotion;
import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import co.istad.rentiq_api.features.promotion.enums.PromotionStatus;
import co.istad.rentiq_api.features.promotion.mapper.PromotionMapper;
import co.istad.rentiq_api.features.promotion.repository.PromotionRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    private static final String VENDOR_ID = "vendor-1";
    private static final String OTHER_VENDOR_ID = "vendor-2";

    @Mock private PromotionRepository promotionRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private WalletService walletService;
    @Mock private PromotionMapper promotionMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private NotificationService notificationService;
    @Mock private PlatformPricingService platformPricingService;

    private PromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionServiceImpl(promotionRepository, itemRepository, walletService, promotionMapper, adminAuditService, notificationService, platformPricingService);
        lenient().when(promotionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Item eligibleItem(UUID id, String ownerId) {
        return Item.builder()
                .id(id).ownerId(ownerId)
                .approvalStatus(ItemApprovalStatus.APPROVED).status(ItemStatus.ACTIVE).deleted(false)
                .build();
    }

    private Promotion activePromotion(String vendorId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return Promotion.builder()
                .id(UUID.randomUUID())
                .vendorId(vendorId)
                .itemId(UUID.randomUUID())
                .packageType(PromotionPackage.BOOST_7_DAYS)
                .durationDays(7)
                .price(new BigDecimal("5.00"))
                .currency("USD")
                .status(PromotionStatus.ACTIVE)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    // ---------------------------------------------------------------
    // Creation
    // ---------------------------------------------------------------

    @Test
    void create_validPurchase_usd_becomesActive_andChargesWalletExactlyOnce() {
        UUID itemId = UUID.randomUUID();
        Item item = eligibleItem(itemId, VENDOR_ID);
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));
        when(promotionRepository.existsEffectiveActiveForItem(eq(itemId), any())).thenReturn(false);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(
                WalletResponse.builder().id(UUID.randomUUID()).ownerId(VENDOR_ID)
                        .balance(new BigDecimal("10.00")).currency("USD").build());
        when(platformPricingService.getPromotionPrice(PromotionPackage.BOOST_7_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("5.00")));

        service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_7_DAYS), VENDOR_ID);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        Promotion saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PromotionStatus.ACTIVE);
        assertThat(saved.getVendorId()).isEqualTo(VENDOR_ID);
        assertThat(saved.getItemId()).isEqualTo(itemId);
        assertThat(saved.getPrice()).isEqualByComparingTo("5.00");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getDurationDays()).isEqualTo(7);
        assertThat(saved.getEndAt()).isEqualTo(saved.getStartAt().plusDays(7));

        verify(walletService, times(1)).chargePromotion(saved.getId(), VENDOR_ID, new BigDecimal("5.00"), "USD");
    }

    @Test
    void create_validPurchase_khr_usesKhrPricing() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(promotionRepository.existsEffectiveActiveForItem(eq(itemId), any())).thenReturn(false);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(
                WalletResponse.builder().id(UUID.randomUUID()).ownerId(VENDOR_ID)
                        .balance(new BigDecimal("50000")).currency("KHR").build());
        when(platformPricingService.getPromotionPrice(PromotionPackage.BOOST_3_DAYS, "KHR"))
                .thenReturn(Optional.of(new BigDecimal("10000")));

        service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_3_DAYS), VENDOR_ID);

        verify(walletService).chargePromotion(any(), eq(VENDOR_ID), eq(new BigDecimal("10000")), eq("KHR"));
    }

    @Test
    void create_rejectsAnotherVendorsItem() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(eligibleItem(itemId, OTHER_VENDOR_ID)));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(promotionRepository, never()).save(any());
        verify(walletService, never()).chargePromotion(any(), any(), any(), any());
    }

    @Test
    void create_rejectsPendingItem() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).ownerId(VENDOR_ID)
                .approvalStatus(ItemApprovalStatus.PENDING).status(ItemStatus.ACTIVE).deleted(false).build();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void create_rejectsRejectedItem() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).ownerId(VENDOR_ID)
                .approvalStatus(ItemApprovalStatus.REJECTED).status(ItemStatus.ACTIVE).deleted(false).build();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void create_rejectsInactiveItem() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).ownerId(VENDOR_ID)
                .approvalStatus(ItemApprovalStatus.APPROVED).status(ItemStatus.HIDDEN).deleted(false).build();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void create_rejectsDeletedItem() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder().id(itemId).ownerId(VENDOR_ID)
                .approvalStatus(ItemApprovalStatus.APPROVED).status(ItemStatus.ACTIVE).deleted(true).build();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void create_rejectsUnknownItem() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void create_rejectsWhenItemAlreadyHasAnActivePromotion() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(promotionRepository.existsEffectiveActiveForItem(eq(itemId), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_3_DAYS), VENDOR_ID))
                .isInstanceOf(InvalidStateException.class);
        verify(promotionRepository, never()).save(any());
        verify(walletService, never()).chargePromotion(any(), any(), any(), any());
    }

    @Test
    void create_rejectsUnsupportedWalletCurrency_beforeChargingOrSaving() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(promotionRepository.existsEffectiveActiveForItem(eq(itemId), any())).thenReturn(false);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(
                WalletResponse.builder().id(UUID.randomUUID()).ownerId(VENDOR_ID)
                        .balance(BigDecimal.TEN).currency("EUR").build());
        when(platformPricingService.getPromotionPrice(PromotionPackage.BOOST_1_DAY, "EUR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_1_DAY), VENDOR_ID))
                .isInstanceOf(InvalidOperationException.class);
        verify(promotionRepository, never()).save(any());
        verify(walletService, never()).chargePromotion(any(), any(), any(), any());
    }

    @Test
    void create_insufficientBalance_propagatesAndNothingSucceeds() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(eligibleItem(itemId, VENDOR_ID)));
        when(promotionRepository.existsEffectiveActiveForItem(eq(itemId), any())).thenReturn(false);
        when(walletService.getWallet(VENDOR_ID)).thenReturn(
                WalletResponse.builder().id(UUID.randomUUID()).ownerId(VENDOR_ID)
                        .balance(new BigDecimal("1.00")).currency("USD").build());
        when(platformPricingService.getPromotionPrice(PromotionPackage.BOOST_7_DAYS, "USD"))
                .thenReturn(Optional.of(new BigDecimal("5.00")));
        org.mockito.Mockito.doThrow(WalletException.insufficientBalance(new BigDecimal("1.00"), new BigDecimal("5.00")))
                .when(walletService).chargePromotion(any(), eq(VENDOR_ID), eq(new BigDecimal("5.00")), eq("USD"));

        assertThatThrownBy(() -> service.create(new CreatePromotionRequest(itemId, PromotionPackage.BOOST_7_DAYS), VENDOR_ID))
                .isInstanceOf(WalletException.class);
        // The Promotion save happens before the charge, in the same @Transactional method —
        // Spring rolls the whole transaction (including this save) back when chargePromotion
        // throws; that rollback guarantee is Spring's, not re-tested here.
    }

    // ---------------------------------------------------------------
    // Cancellation — no refund, terminal-state guards
    // ---------------------------------------------------------------

    @Test
    void cancel_ownActivePromotion_succeeds_noWalletInteraction() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        service.cancel(promotion.getId(), VENDOR_ID);

        assertThat(promotion.getStatus()).isEqualTo(PromotionStatus.CANCELLED);
        assertThat(promotion.getCancelledAt()).isNotNull();
        verifyNoInteractions(walletService);
    }

    @Test
    void cancel_anotherVendorsPromotion_rejected() {
        Promotion promotion = activePromotion(OTHER_VENDOR_ID, OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.cancel(promotion.getId(), VENDOR_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_effectivelyExpiredPromotion_rejected() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now().minusDays(10), OffsetDateTime.now().minusDays(1));
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.cancel(promotion.getId(), VENDOR_ID)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cancel_suspendedPromotion_rejected() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        promotion.setStatus(PromotionStatus.SUSPENDED);
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.cancel(promotion.getId(), VENDOR_ID)).isInstanceOf(InvalidStateException.class);
    }

    // ---------------------------------------------------------------
    // Detail / ownership
    // ---------------------------------------------------------------

    @Test
    void getById_owner_succeeds() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.of(promotion));

        service.getById(promotion.getId(), VENDOR_ID, false);

        verify(promotionMapper).toResponse(promotion);
    }

    @Test
    void getById_otherVendor_rejected() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.getById(promotion.getId(), OTHER_VENDOR_ID, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_admin_alwaysAllowed() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));
        when(promotionRepository.findById(promotion.getId())).thenReturn(Optional.of(promotion));

        service.getById(promotion.getId(), "admin-1", true);

        verify(promotionMapper).toResponse(promotion);
    }

    // ---------------------------------------------------------------
    // Admin suspension
    // ---------------------------------------------------------------

    @Test
    void adminSuspend_activePromotion_succeeds_andAudits() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now().minusHours(1), OffsetDateTime.now().plusDays(5));
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        service.adminSuspend(promotion.getId(), new SuspendPromotionRequest(PromotionStatus.SUSPENDED, "Policy violation"), "admin-1");

        assertThat(promotion.getStatus()).isEqualTo(PromotionStatus.SUSPENDED);
        assertThat(promotion.getSuspendedBy()).isEqualTo("admin-1");
        assertThat(promotion.getSuspensionReason()).isEqualTo("Policy violation");
        verify(adminAuditService).record(
                AdminAuditAction.PROMOTION_SUSPENDED, AdminAuditTargetType.PROMOTION, promotion.getId().toString(),
                Map.of("status", "ACTIVE"), Map.of("status", "SUSPENDED"), "Policy violation");
        verify(notificationService).notifyUser(
                eq(VENDOR_ID), eq(co.istad.rentiq_api.features.notification.enums.NotificationType.PROMOTION), any(), any(),
                eq(co.istad.rentiq_api.features.notification.enums.NotificationReferenceType.PROMOTION), eq(promotion.getId()));
    }

    @Test
    void adminSuspend_alreadyExpiredPromotion_rejected_noAudit() {
        Promotion promotion = activePromotion(VENDOR_ID, OffsetDateTime.now().minusDays(10), OffsetDateTime.now().minusDays(1));
        when(promotionRepository.findByIdForUpdate(promotion.getId())).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.adminSuspend(
                promotion.getId(), new SuspendPromotionRequest(PromotionStatus.SUSPENDED, "reason"), "admin-1"))
                .isInstanceOf(InvalidStateException.class);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminSuspend_rejectsNonSuspendedTargetStatus_cannotArbitrarilyChangeStatus() {
        UUID promotionId = UUID.randomUUID();

        // Rejected before the promotion is even loaded — an admin cannot use this endpoint to
        // set any status other than SUSPENDED, regardless of what the promotion looks like.
        assertThatThrownBy(() -> service.adminSuspend(
                promotionId, new SuspendPromotionRequest(PromotionStatus.CANCELLED, "reason"), "admin-1"))
                .isInstanceOf(InvalidOperationException.class);
        verify(promotionRepository, never()).findByIdForUpdate(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Impressions / clicks
    // ---------------------------------------------------------------

    @Test
    void recordImpression_unknownPromotion_notFound() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.recordImpression(id)).isInstanceOf(NotFoundException.class);
        verify(promotionRepository, never()).incrementImpressionIfActive(any(), any());
    }

    @Test
    void recordImpression_existingPromotion_callsAtomicIncrement() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.existsById(id)).thenReturn(true);

        service.recordImpression(id);

        verify(promotionRepository).incrementImpressionIfActive(eq(id), any());
    }

    @Test
    void recordClick_existingPromotion_callsAtomicIncrement() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.existsById(id)).thenReturn(true);

        service.recordClick(id);

        verify(promotionRepository).incrementClickIfActive(eq(id), any());
    }

    @Test
    void recordClick_unknownPromotion_notFound() {
        UUID id = UUID.randomUUID();
        when(promotionRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.recordClick(id)).isInstanceOf(NotFoundException.class);
        verify(promotionRepository, never()).incrementClickIfActive(any(), any());
    }

    // ---------------------------------------------------------------
    // Admin list delegation
    // ---------------------------------------------------------------

    @Test
    void adminList_delegatesToSpecificationBasedQuery() {
        Pageable pageable = mock(Pageable.class);
        when(promotionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.adminList(PromotionStatus.ACTIVE, VENDOR_ID, null, PromotionPackage.BOOST_7_DAYS, null, null, pageable);

        verify(promotionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    private void verifyNoInteractions(Object mock) {
        org.mockito.Mockito.verifyNoInteractions(mock);
    }
}
