package co.istad.rentiq_api.features.platformSetting.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.platformSetting.dto.request.UpdatePlatformSettingRequest;
import co.istad.rentiq_api.features.platformSetting.dto.response.PlatformSettingResponse;
import co.istad.rentiq_api.features.platformSetting.entity.PlatformSetting;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import co.istad.rentiq_api.features.platformSetting.enums.SettingCategory;
import co.istad.rentiq_api.features.platformSetting.repository.PlatformSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformSettingServiceImplTest {

    private static final String ADMIN_ID = "admin-1";

    @Mock private PlatformSettingRepository platformSettingRepository;
    @Mock private AdminAuditService adminAuditService;

    private PlatformSettingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlatformSettingServiceImpl(platformSettingRepository, adminAuditService);
        lenient().when(platformSettingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------
    // Defaults — empty settings table must reproduce today's hard-coded prices exactly
    // ---------------------------------------------------------------

    @Test
    void getAllSettings_emptyTable_returnsAllTwelveKeysWithDefaults_noneOverridden() {
        when(platformSettingRepository.findAll()).thenReturn(List.of());

        List<PlatformSettingResponse> settings = service.getAllSettings();

        assertThat(settings).hasSize(12);
        assertThat(settings).allMatch(s -> !s.overridden());
        assertThat(settings).allMatch(s -> s.value().compareTo(s.defaultValue()) == 0);

        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_1_DAY_USD).value()).isEqualByComparingTo("1.00");
        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR).value()).isEqualByComparingTo("4000");
        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_3_DAYS_USD).value()).isEqualByComparingTo("2.50");
        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_3_DAYS_KHR).value()).isEqualByComparingTo("10000");
        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD).value()).isEqualByComparingTo("5.00");
        assertThat(find(settings, PlatformSettingKey.PROMOTION_BOOST_7_DAYS_KHR).value()).isEqualByComparingTo("20000");

        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_USD).value()).isEqualByComparingTo("3.00");
        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_KHR).value()).isEqualByComparingTo("12000");
        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD).value()).isEqualByComparingTo("6.00");
        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_KHR).value()).isEqualByComparingTo("24000");
        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_14_DAYS_USD).value()).isEqualByComparingTo("10.00");
        assertThat(find(settings, PlatformSettingKey.ADVERTISEMENT_AD_14_DAYS_KHR).value()).isEqualByComparingTo("40000");
    }

    @Test
    void getAllSettings_withOneOverride_onlyThatKeyReflectsIt() {
        PlatformSetting override = PlatformSetting.builder()
                .key(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD)
                .value(new BigDecimal("6.00"))
                .updatedBy(ADMIN_ID)
                .build();
        when(platformSettingRepository.findAll()).thenReturn(List.of(override));

        List<PlatformSettingResponse> settings = service.getAllSettings();

        PlatformSettingResponse overridden = find(settings, PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD);
        assertThat(overridden.overridden()).isTrue();
        assertThat(overridden.value()).isEqualByComparingTo("6.00");
        assertThat(overridden.defaultValue()).isEqualByComparingTo("5.00");
        assertThat(overridden.updatedBy()).isEqualTo(ADMIN_ID);

        long overriddenCount = settings.stream().filter(PlatformSettingResponse::overridden).count();
        assertThat(overriddenCount).isEqualTo(1);
    }

    @Test
    void getEffectiveValue_noOverride_returnsDefault() {
        when(platformSettingRepository.findById(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD)).thenReturn(Optional.empty());

        assertThat(service.getEffectiveValue(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD)).isEqualByComparingTo("6.00");
    }

    @Test
    void getEffectiveValue_withOverride_returnsOverride() {
        when(platformSettingRepository.findById(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD)).thenReturn(Optional.of(
                PlatformSetting.builder().key(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD).value(new BigDecimal("8.00")).build()));

        assertThat(service.getEffectiveValue(PlatformSettingKey.ADVERTISEMENT_AD_7_DAYS_USD)).isEqualByComparingTo("8.00");
    }

    @Test
    void getSetting_unknownOverride_returnsDefaultResponse() {
        when(platformSettingRepository.findById(PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR)).thenReturn(Optional.empty());

        PlatformSettingResponse response = service.getSetting(PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR);

        assertThat(response.key()).isEqualTo(PlatformSettingKey.PROMOTION_BOOST_1_DAY_KHR);
        assertThat(response.category()).isEqualTo(SettingCategory.PROMOTION);
        assertThat(response.currency()).isEqualTo("KHR");
        assertThat(response.overridden()).isFalse();
        assertThat(response.value()).isEqualByComparingTo("4000");
    }

    // ---------------------------------------------------------------
    // Update — validation
    // ---------------------------------------------------------------

    @Test
    void update_validOverride_savesRow_andAudits() {
        when(platformSettingRepository.findByKeyForUpdate(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD)).thenReturn(Optional.empty());
        when(platformSettingRepository.findById(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD)).thenReturn(Optional.empty());

        PlatformSettingResponse response = service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("6.00"), "Updated platform pricing"),
                ADMIN_ID);

        assertThat(response.value()).isEqualByComparingTo("6.00");
        assertThat(response.overridden()).isTrue();

        ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
        verify(platformSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualByComparingTo("6.00");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(ADMIN_ID);

        verify(adminAuditService).record(
                org.mockito.ArgumentMatchers.eq(AdminAuditAction.PLATFORM_SETTING_UPDATED),
                org.mockito.ArgumentMatchers.eq(AdminAuditTargetType.PLATFORM_SETTING),
                org.mockito.ArgumentMatchers.eq(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD.name()),
                any(), any(),
                org.mockito.ArgumentMatchers.eq("Updated platform pricing"));
    }

    @Test
    void update_zeroPrice_rejected_noSaveNoAudit() {
        assertThatThrownBy(() -> service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(BigDecimal.ZERO, "reason"),
                ADMIN_ID))
                .isInstanceOf(InvalidOperationException.class);

        verify(platformSettingRepository, never()).save(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_negativePrice_rejected() {
        assertThatThrownBy(() -> service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("-1.00"), "reason"),
                ADMIN_ID))
                .isInstanceOf(InvalidOperationException.class);
        verify(platformSettingRepository, never()).save(any());
    }

    @Test
    void update_usdMoreThanTwoDecimals_rejected() {
        assertThatThrownBy(() -> service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("5.555"), "reason"),
                ADMIN_ID))
                .isInstanceOf(InvalidOperationException.class);
        verify(platformSettingRepository, never()).save(any());
    }

    @Test
    void update_usdWholeOrTwoDecimals_accepted() {
        // ADVERTISEMENT_AD_3_DAYS_USD's default is 3.00 — none of these values coincide with the
        // default, so every one takes the override/save path (not the reset-to-default path).
        when(platformSettingRepository.findByKeyForUpdate(any())).thenReturn(Optional.empty());
        when(platformSettingRepository.findById(any())).thenReturn(Optional.empty());

        service.update(PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("5"), "reason"), ADMIN_ID);
        service.update(PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("5.5"), "reason"), ADMIN_ID);
        service.update(PlatformSettingKey.ADVERTISEMENT_AD_3_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("5.50"), "reason"), ADMIN_ID);

        verify(platformSettingRepository, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void update_khrFractional_rejected() {
        assertThatThrownBy(() -> service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_KHR,
                new UpdatePlatformSettingRequest(new BigDecimal("4000.50"), "reason"),
                ADMIN_ID))
                .isInstanceOf(InvalidOperationException.class);
        verify(platformSettingRepository, never()).save(any());
    }

    @Test
    void update_khrWholeNumber_accepted() {
        when(platformSettingRepository.findByKeyForUpdate(any())).thenReturn(Optional.empty());
        when(platformSettingRepository.findById(any())).thenReturn(Optional.empty());

        service.update(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_KHR,
                new UpdatePlatformSettingRequest(new BigDecimal("12500"), "reason"), ADMIN_ID);

        verify(platformSettingRepository).save(any());
    }

    @Test
    void update_toExactlyDefaultValue_removesExistingOverride_ratherThanStoringIt() {
        PlatformSetting existing = PlatformSetting.builder()
                .key(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD)
                .value(new BigDecimal("6.00"))
                .updatedBy(ADMIN_ID)
                .build();
        when(platformSettingRepository.findByKeyForUpdate(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(Optional.of(existing));
        when(platformSettingRepository.findById(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(Optional.of(existing));

        PlatformSettingResponse response = service.update(
                PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("5.00"), "Reverting to default"),
                ADMIN_ID);

        verify(platformSettingRepository).delete(existing);
        verify(platformSettingRepository, never()).save(any());
        assertThat(response.overridden()).isFalse();
        assertThat(response.value()).isEqualByComparingTo("5.00");
    }

    @Test
    void update_usesLockedLookup_forConcurrencySafety() {
        when(platformSettingRepository.findByKeyForUpdate(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(Optional.empty());
        when(platformSettingRepository.findById(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD))
                .thenReturn(Optional.empty());

        service.update(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD,
                new UpdatePlatformSettingRequest(new BigDecimal("6.00"), "reason"), ADMIN_ID);

        verify(platformSettingRepository).findByKeyForUpdate(PlatformSettingKey.PROMOTION_BOOST_7_DAYS_USD);
    }

    private PlatformSettingResponse find(List<PlatformSettingResponse> settings, PlatformSettingKey key) {
        return settings.stream().filter(s -> s.key() == key).findFirst()
                .orElseThrow(() -> new AssertionError("Missing setting: " + key));
    }
}
