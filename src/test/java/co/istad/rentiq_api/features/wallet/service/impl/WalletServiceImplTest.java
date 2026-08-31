package co.istad.rentiq_api.features.wallet.service.impl;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.service.NotificationService;
import co.istad.rentiq_api.features.vendorApplication.entity.VendorApplication;
import co.istad.rentiq_api.features.vendorApplication.enums.VendorApplicationStatus;
import co.istad.rentiq_api.features.vendorApplication.repository.VendorApplicationRepository;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletAdjustRequest;
import co.istad.rentiq_api.features.wallet.dto.request.AdminWalletTopupRequest;
import co.istad.rentiq_api.features.wallet.dto.response.AdminWalletTopupResponse;
import co.istad.rentiq_api.features.wallet.entity.OwnerWallet;
import co.istad.rentiq_api.features.wallet.entity.WalletTransaction;
import co.istad.rentiq_api.features.wallet.enums.TransactionDirection;
import co.istad.rentiq_api.features.wallet.enums.WalletStatus;
import co.istad.rentiq_api.features.wallet.exception.WalletException;
import co.istad.rentiq_api.features.wallet.mapper.WalletMapper;
import co.istad.rentiq_api.features.wallet.repository.OwnerWalletRepository;
import co.istad.rentiq_api.features.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final String OWNER_ID = "vendor-1";

    @Mock private OwnerWalletRepository walletRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private WalletMapper walletMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private VendorApplicationRepository vendorApplicationRepository;
    @Mock private NotificationService notificationService;

    private WalletServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(
                walletRepository, transactionRepository, walletMapper,
                adminAuditService, vendorApplicationRepository, notificationService);

        lenient().when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> {
            WalletTransaction transaction = invocation.getArgument(0);
            transaction.setId(UUID.randomUUID());
            transaction.setCreatedAt(OffsetDateTime.now());
            return transaction;
        });
    }

    private OwnerWallet wallet(UUID id, String currency, BigDecimal balance) {
        return OwnerWallet.builder().id(id).ownerId(OWNER_ID).currency(currency).balance(balance).build();
    }

    private VendorApplication approvedApplication() {
        return VendorApplication.builder().userId(OWNER_ID).status(VendorApplicationStatus.APPROVED).build();
    }

    // ---------------------------------------------------------------
    // Admin direct top-up
    // ---------------------------------------------------------------

    @Test
    void adminTopupWallet_creditsUsdWallet_exactlyOnce_andRecordsAudit() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("10.00"));

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(vendorApplicationRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(approvedApplication()));

        AdminWalletTopupResponse response = service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("5.00"), "USD", "ABA", "ref-1", "Vendor paid admin externally"),
                "admin-1");

        assertThat(wallet.getBalance()).isEqualByComparingTo("15.00");
        assertThat(response.balanceBefore()).isEqualByComparingTo("10.00");
        assertThat(response.balanceAfter()).isEqualByComparingTo("15.00");
        assertThat(response.amount()).isEqualByComparingTo("5.00");
        assertThat(response.currency()).isEqualTo("USD");

        verify(transactionRepository, org.mockito.Mockito.times(1)).save(any(WalletTransaction.class));
        verify(adminAuditService).record(
                AdminAuditAction.WALLET_TOPPED_UP,
                AdminAuditTargetType.WALLET,
                walletId.toString(),
                Map.of("balance", new BigDecimal("10.00")),
                Map.of("balance", new BigDecimal("15.00"), "topUpAmount", new BigDecimal("5.00"), "currency", "USD"),
                "Vendor paid admin externally");
        verify(notificationService).notifyUser(
                eq(OWNER_ID), eq(NotificationType.PAYMENT), any(),
                org.mockito.ArgumentMatchers.contains("5.00"), eq(NotificationReferenceType.PAYMENT), any());
    }

    @Test
    void adminTopupWallet_creditsKhrWallet_whenCurrencyMatches() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "KHR", new BigDecimal("20000"));

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(vendorApplicationRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(approvedApplication()));

        service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("10000"), "KHR", "KHQR", null, null),
                "admin-1");

        assertThat(wallet.getBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void adminTopupWallet_rejectsCurrencyMismatch_noConversion() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", BigDecimal.ZERO);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(vendorApplicationRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(approvedApplication()));

        assertThatThrownBy(() -> service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("10000"), "KHR", null, null, null), "admin-1"))
                .isInstanceOf(WalletException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(transactionRepository, never()).save(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminTopupWallet_rejectsUnknownWallet() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("5.00"), "USD", null, null, null), "admin-1"))
                .isInstanceOf(WalletException.class);
    }

    @Test
    void adminTopupWallet_rejectsWalletWithoutApprovedVendorApplication() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", BigDecimal.ZERO);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(vendorApplicationRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("5.00"), "USD", null, null, null), "admin-1"))
                .isInstanceOf(WalletException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void adminTopupWallet_rejectsWalletWithNonApprovedVendorApplication() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", BigDecimal.ZERO);
        VendorApplication pending = VendorApplication.builder().userId(OWNER_ID).status(VendorApplicationStatus.PENDING).build();

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(vendorApplicationRepository.findByUserId(OWNER_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.adminTopupWallet(walletId,
                new AdminWalletTopupRequest(new BigDecimal("5.00"), "USD", null, null, null), "admin-1"))
                .isInstanceOf(WalletException.class);
    }

    // ---------------------------------------------------------------
    // Admin manual adjustment (kept conceptually separate from TOP_UP)
    // ---------------------------------------------------------------

    @Test
    void adminAdjustWallet_credit_recordsWalletCreditedAuditWithBalances() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = OwnerWallet.builder().id(walletId).balance(new BigDecimal("100.00")).build();

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        service.adminAdjustWallet(walletId,
                new AdminWalletAdjustRequest(new BigDecimal("20.00"), TransactionDirection.IN, "Manual correction"),
                "admin-1");

        verify(adminAuditService).record(
                AdminAuditAction.WALLET_CREDITED,
                AdminAuditTargetType.WALLET,
                walletId.toString(),
                Map.of("balance", new BigDecimal("100.00")),
                Map.of("balance", new BigDecimal("120.00"), "adjustmentAmount", new BigDecimal("20.00")),
                "Manual correction");
    }

    @Test
    void adminAdjustWallet_debitBelowBalance_succeeds() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = OwnerWallet.builder().id(walletId).balance(new BigDecimal("100.00")).build();
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        service.adminAdjustWallet(walletId,
                new AdminWalletAdjustRequest(new BigDecimal("30.00"), TransactionDirection.OUT, "Chargeback"),
                "admin-1");

        assertThat(wallet.getBalance()).isEqualByComparingTo("70.00");
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
        verify(adminAuditService).record(
                eq(AdminAuditAction.WALLET_DEBITED), eq(AdminAuditTargetType.WALLET), eq(walletId.toString()),
                any(), any(), eq("Chargeback"));
    }

    @Test
    void adminAdjustWallet_debitEqualToBalance_locksWalletAtZero() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = OwnerWallet.builder().id(walletId).balance(new BigDecimal("50.00")).build();
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        service.adminAdjustWallet(walletId,
                new AdminWalletAdjustRequest(new BigDecimal("50.00"), TransactionDirection.OUT, "Full withdrawal"),
                "admin-1");

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.LOCKED);
    }

    @Test
    void adminAdjustWallet_debitGreaterThanBalance_rejected_negativeBalanceNeverPersisted() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = OwnerWallet.builder().id(walletId).balance(new BigDecimal("50.00")).build();
        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.adminAdjustWallet(walletId,
                new AdminWalletAdjustRequest(new BigDecimal("100.00"), TransactionDirection.OUT, "Too much"),
                "admin-1"))
                .isInstanceOf(WalletException.class);

        // Nothing partially applied: balance is untouched, no ledger row, no audit.
        assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");
        verify(transactionRepository, never()).save(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Legacy top-up flow removed (backend audit SEC-001/BUS-001) — WalletServiceImpl no
    // longer has createTopupRequest/getTopupRequests/getTopupRequest/processTopupWebhook/
    // adminListTopupRequests/adminConfirmTopup at all. Their absence is proven structurally:
    // this test class compiles against the full WalletService contract (see the constructor
    // call above) with none of those methods available to call. adminTopupWallet (tested
    // above) is the only method in WalletService capable of producing a TOP_UP/IN ledger row.
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Promotion charge
    // ---------------------------------------------------------------

    @Test
    void chargePromotion_debitsWallet_withPromotionTypeAndOutDirection() {
        UUID walletId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("10.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        service.chargePromotion(promotionId, OWNER_ID, new BigDecimal("5.00"), "USD");

        assertThat(wallet.getBalance()).isEqualByComparingTo("5.00");

        org.mockito.ArgumentCaptor<WalletTransaction> captor = org.mockito.ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(captor.capture());
        WalletTransaction transaction = captor.getValue();
        assertThat(transaction.getTransactionType())
                .isEqualTo(co.istad.rentiq_api.features.wallet.enums.TransactionType.PROMOTION);
        assertThat(transaction.getDirection()).isEqualTo(TransactionDirection.OUT);
        assertThat(transaction.getAmount()).isEqualByComparingTo("5.00");
        assertThat(transaction.getPromotionId()).isEqualTo(promotionId);
    }

    @Test
    void chargePromotion_insufficientBalance_rejectsAndLeavesWalletUnchanged() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("1.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.chargePromotion(UUID.randomUUID(), OWNER_ID, new BigDecimal("5.00"), "USD"))
                .isInstanceOf(WalletException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("1.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void chargePromotion_currencyMismatch_rejectsWithoutConverting() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("100.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.chargePromotion(UUID.randomUUID(), OWNER_ID, new BigDecimal("10000"), "KHR"))
                .isInstanceOf(WalletException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Advertisement charge
    // ---------------------------------------------------------------

    @Test
    void chargeAdvertisement_debitsWallet_withAdvertisementTypeAndOutDirection() {
        UUID walletId = UUID.randomUUID();
        UUID advertisementId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("10.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        service.chargeAdvertisement(advertisementId, OWNER_ID, new BigDecimal("6.00"), "USD");

        assertThat(wallet.getBalance()).isEqualByComparingTo("4.00");

        org.mockito.ArgumentCaptor<WalletTransaction> captor = org.mockito.ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(captor.capture());
        WalletTransaction transaction = captor.getValue();
        assertThat(transaction.getTransactionType())
                .isEqualTo(co.istad.rentiq_api.features.wallet.enums.TransactionType.ADVERTISEMENT);
        assertThat(transaction.getDirection()).isEqualTo(TransactionDirection.OUT);
        assertThat(transaction.getAmount()).isEqualByComparingTo("6.00");
        assertThat(transaction.getAdvertisementId()).isEqualTo(advertisementId);
    }

    @Test
    void chargeAdvertisement_insufficientBalance_rejectsAndLeavesWalletUnchanged() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("3.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.chargeAdvertisement(UUID.randomUUID(), OWNER_ID, new BigDecimal("6.00"), "USD"))
                .isInstanceOf(WalletException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("3.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void chargeAdvertisement_currencyMismatch_rejectsWithoutConverting() {
        UUID walletId = UUID.randomUUID();
        OwnerWallet wallet = wallet(walletId, "USD", new BigDecimal("100.00"));
        when(walletRepository.findByOwnerIdForUpdate(OWNER_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.chargeAdvertisement(UUID.randomUUID(), OWNER_ID, new BigDecimal("24000"), "KHR"))
                .isInstanceOf(WalletException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }
}
