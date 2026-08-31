package co.istad.rentiq_api.features.localization.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * The complete, closed set of locales this MVP serves UI strings for. Adding a language means
 * adding a constant here plus its resource file — never resolving an arbitrary client-supplied
 * code. Unsupported codes must fail cleanly (see LocalizationService), never silently fall back
 * to English.
 */
public enum SupportedLocale {

    EN("en", "English", "English"),
    KM("km", "Khmer", "ខ្មែរ");

    private final String code;
    private final String name;
    private final String nativeName;

    SupportedLocale(String code, String name, String nativeName) {
        this.code = code;
        this.name = name;
        this.nativeName = nativeName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNativeName() {
        return nativeName;
    }

    /**
     * Case-insensitive lookup — "EN", "En", "en" all resolve to {@link #EN}. Empty when the
     * code isn't one of the supported locales; callers must reject, never default to English.
     */
    public static Optional<SupportedLocale> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toLowerCase();
        return Arrays.stream(values()).filter(locale -> locale.code.equals(normalized)).findFirst();
    }
}
