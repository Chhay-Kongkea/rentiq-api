package co.istad.rentiq_api.features.localization.service.impl;

import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.localization.dto.response.LocaleResponse;
import co.istad.rentiq_api.features.localization.dto.response.LocaleStringsResponse;
import co.istad.rentiq_api.features.localization.enums.SupportedLocale;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalizationServiceImplTest {

    private LocalizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocalizationServiceImpl(new ObjectMapper());
        service.init();
    }

    // ---------------------------------------------------------------
    // Supported locales
    // ---------------------------------------------------------------

    @Test
    void getSupportedLocales_returnsExactlyEnglishAndKhmer_noDuplicates() {
        List<LocaleResponse> locales = service.getSupportedLocales();

        assertThat(locales).extracting(LocaleResponse::code).containsExactlyInAnyOrder("en", "km");
        assertThat(locales).extracting(LocaleResponse::code).doesNotHaveDuplicates();
    }

    @Test
    void getSupportedLocales_englishMetadata_isCorrect() {
        LocaleResponse en = find(service.getSupportedLocales(), "en");

        assertThat(en.name()).isEqualTo("English");
        assertThat(en.nativeName()).isEqualTo("English");
    }

    @Test
    void getSupportedLocales_khmerMetadata_isCorrect() {
        LocaleResponse km = find(service.getSupportedLocales(), "km");

        assertThat(km.name()).isEqualTo("Khmer");
        assertThat(km.nativeName()).isEqualTo("ខ្មែរ");
    }

    private LocaleResponse find(List<LocaleResponse> locales, String code) {
        return locales.stream().filter(l -> l.code().equals(code)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing locale: " + code));
    }

    // ---------------------------------------------------------------
    // English strings
    // ---------------------------------------------------------------

    @Test
    void getStrings_english_returnsExpectedCoreKeys() {
        LocaleStringsResponse response = service.getStrings("en");

        assertThat(response.code()).isEqualTo("en");
        assertThat(response.strings()).containsEntry("common.save", "Save");
        assertThat(response.strings()).containsEntry("status.approved", "Approved");
        assertThat(response.strings()).containsEntry("wallet.balance", "Balance");
        assertThat(response.strings()).containsEntry("revenue.totalRevenue", "Total Revenue");
    }

    // ---------------------------------------------------------------
    // Khmer strings — UTF-8 survives loading
    // ---------------------------------------------------------------

    @Test
    void getStrings_khmer_returnsExpectedUtf8Values() {
        LocaleStringsResponse response = service.getStrings("km");

        assertThat(response.code()).isEqualTo("km");
        assertThat(response.strings()).containsEntry("common.save", "រក្សាទុក");
        assertThat(response.strings()).containsEntry("common.cancel", "បោះបង់");
        assertThat(response.strings()).containsEntry("status.approved", "បានអនុម័ត");
        assertThat(response.strings()).containsEntry("status.pending", "កំពុងរង់ចាំ");
    }

    // ---------------------------------------------------------------
    // Case insensitivity
    // ---------------------------------------------------------------

    @Test
    void getStrings_caseInsensitiveCode_resolvesToSameLocale() {
        assertThat(service.getStrings("EN").code()).isEqualTo("en");
        assertThat(service.getStrings("En").code()).isEqualTo("en");
        assertThat(service.getStrings("KM").code()).isEqualTo("km");
        assertThat(service.getStrings("Km").code()).isEqualTo("km");
    }

    // ---------------------------------------------------------------
    // Unsupported locale
    // ---------------------------------------------------------------

    @Test
    void getStrings_unsupportedLocale_throwsNotFound_noEnglishFallback() {
        assertThatThrownBy(() -> service.getStrings("fr")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getStrings_blankCode_throwsNotFound() {
        assertThatThrownBy(() -> service.getStrings("")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getStrings_nullCode_throwsNotFound() {
        assertThatThrownBy(() -> service.getStrings(null)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------
    // Key parity between English and Khmer — the real resource files
    // ---------------------------------------------------------------

    @Test
    void realResourceFiles_englishAndKhmerKeySets_areIdentical() {
        LocaleStringsResponse en = service.getStrings("en");
        LocaleStringsResponse km = service.getStrings("km");

        assertThat(km.strings().keySet()).containsExactlyInAnyOrderElementsOf(en.strings().keySet());
    }

    @Test
    void realResourceFiles_haveNoBlankKeysOrValues() {
        for (String code : List.of("en", "km")) {
            Map<String, String> strings = service.getStrings(code).strings();
            for (Map.Entry<String, String> entry : strings.entrySet()) {
                assertThat(entry.getKey()).isNotBlank();
                assertThat(entry.getValue()).isNotBlank();
            }
        }
    }

    // ---------------------------------------------------------------
    // Validation logic in isolation
    // ---------------------------------------------------------------

    @Test
    void validateKeyParity_matchingKeys_doesNotThrow() {
        Map<String, String> en = Map.of("a", "A", "b", "B");
        Map<String, String> km = Map.of("a", "1", "b", "2");

        LocalizationServiceImpl.validateKeyParity(SupportedLocale.EN, en, SupportedLocale.KM, km);
    }

    @Test
    void validateKeyParity_missingKeyInTarget_throws() {
        Map<String, String> en = Map.of("a", "A", "b", "B");
        Map<String, String> km = Map.of("a", "1");

        assertThatThrownBy(() ->
                LocalizationServiceImpl.validateKeyParity(SupportedLocale.EN, en, SupportedLocale.KM, km))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("b");
    }

    @Test
    void validateKeyParity_extraKeyInTarget_throws() {
        Map<String, String> en = Map.of("a", "A");
        Map<String, String> km = Map.of("a", "1", "extra", "2");

        assertThatThrownBy(() ->
                LocalizationServiceImpl.validateKeyParity(SupportedLocale.EN, en, SupportedLocale.KM, km))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("extra");
    }

    @Test
    void validateNoBlanks_blankValue_throws() {
        Map<String, String> strings = new LinkedHashMap<>();
        strings.put("common.save", "");

        assertThatThrownBy(() -> LocalizationServiceImpl.validateNoBlanks(SupportedLocale.EN, strings))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateNoBlanks_nullValue_throws() {
        Map<String, String> strings = new LinkedHashMap<>();
        strings.put("common.save", null);

        assertThatThrownBy(() -> LocalizationServiceImpl.validateNoBlanks(SupportedLocale.EN, strings))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateNoBlanks_blankKey_throws() {
        Map<String, String> strings = new LinkedHashMap<>();
        strings.put(" ", "value");

        assertThatThrownBy(() -> LocalizationServiceImpl.validateNoBlanks(SupportedLocale.EN, strings))
                .isInstanceOf(IllegalStateException.class);
    }
}
