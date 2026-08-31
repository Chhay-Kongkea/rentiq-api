package co.istad.rentiq_api.features.platformPricing.dto.response;

import java.util.List;

/**
 * Public, read-only view of current effective package pricing. Deliberately excludes
 * updatedBy/audit/override metadata — that belongs to the Admin-only Settings API, not here.
 */
public record PlatformPricingResponse(
        List<PackagePricingResponse> promotions,
        List<PackagePricingResponse> advertisements
) {}
