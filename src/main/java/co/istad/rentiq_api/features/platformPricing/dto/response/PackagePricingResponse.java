package co.istad.rentiq_api.features.platformPricing.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record PackagePricingResponse(
        String packageType,
        int durationDays,
        Map<String, BigDecimal> prices
) {}
