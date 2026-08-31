package co.istad.rentiq_api.features.localization.service.impl;

import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.localization.dto.response.LocaleResponse;
import co.istad.rentiq_api.features.localization.dto.response.LocaleStringsResponse;
import co.istad.rentiq_api.features.localization.enums.SupportedLocale;
import co.istad.rentiq_api.features.localization.service.LocalizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the static, UTF-8 JSON translation resources once at startup, validates them (English
 * is the canonical key set — Khmer must match exactly, no blank keys/values), and serves them
 * from an immutable in-memory map. Never reads the classpath resource per request; never
 * touches a database.
 */
@Service
@RequiredArgsConstructor
public class LocalizationServiceImpl implements LocalizationService {

    private static final String RESOURCE_PATH_TEMPLATE = "i18n/strings_%s.json";

    private final ObjectMapper objectMapper;

    private Map<SupportedLocale, Map<String, String>> stringsByLocale;

    @PostConstruct
    void init() {
        Map<SupportedLocale, Map<String, String>> loaded = new EnumMap<>(SupportedLocale.class);
        for (SupportedLocale locale : SupportedLocale.values()) {
            loaded.put(locale, loadResource(locale));
        }

        Map<String, String> reference = loaded.get(SupportedLocale.EN);
        for (SupportedLocale locale : SupportedLocale.values()) {
            validateNoBlanks(locale, loaded.get(locale));
            if (locale != SupportedLocale.EN) {
                validateKeyParity(SupportedLocale.EN, reference, locale, loaded.get(locale));
            }
        }

        this.stringsByLocale = Collections.unmodifiableMap(loaded);
    }

    private Map<String, String> loadResource(SupportedLocale locale) {
        String path = RESOURCE_PATH_TEMPLATE.formatted(locale.getCode());
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream in = resource.getInputStream()) {
            Map<String, String> strings = objectMapper.readValue(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, String>>() {});
            return Collections.unmodifiableMap(strings);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load localization resource: " + path, e);
        }
    }

    static void validateNoBlanks(SupportedLocale locale, Map<String, String> strings) {
        for (Map.Entry<String, String> entry : strings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalStateException("Blank translation key found in locale " + locale.getCode());
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalStateException(
                        "Blank translation value for key '" + entry.getKey() + "' in locale " + locale.getCode());
            }
        }
    }

    static void validateKeyParity(
            SupportedLocale referenceLocale, Map<String, String> reference,
            SupportedLocale locale, Map<String, String> strings
    ) {
        if (reference.keySet().equals(strings.keySet())) {
            return;
        }

        Set<String> missing = new LinkedHashSet<>(reference.keySet());
        missing.removeAll(strings.keySet());
        Set<String> extra = new LinkedHashSet<>(strings.keySet());
        extra.removeAll(reference.keySet());

        throw new IllegalStateException(
                "Locale '%s' key set does not match reference locale '%s'. Missing: %s. Extra: %s."
                        .formatted(locale.getCode(), referenceLocale.getCode(), missing, extra));
    }

    @Override
    public List<LocaleResponse> getSupportedLocales() {
        return Arrays.stream(SupportedLocale.values())
                .map(locale -> new LocaleResponse(locale.getCode(), locale.getName(), locale.getNativeName()))
                .toList();
    }

    @Override
    public LocaleStringsResponse getStrings(String code) {
        SupportedLocale locale = SupportedLocale.fromCode(code)
                .orElseThrow(() -> new NotFoundException("Locale", code));

        Map<String, String> strings = new LinkedHashMap<>(stringsByLocale.get(locale));
        return new LocaleStringsResponse(locale.getCode(), Collections.unmodifiableMap(strings));
    }
}
