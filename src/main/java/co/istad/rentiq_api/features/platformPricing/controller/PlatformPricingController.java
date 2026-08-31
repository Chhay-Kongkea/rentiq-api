package co.istad.rentiq_api.features.platformPricing.controller;

import co.istad.rentiq_api.features.platformPricing.dto.response.PlatformPricingResponse;
import co.istad.rentiq_api.features.platformPricing.service.PlatformPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only current pricing — no mutation endpoints, no Admin behavior. Backed by the
 * exact same PlatformPricingService used by Promotion purchase and Advertisement quoting, so
 * the frontend never hard-codes package prices and never sees a number this service didn't
 * itself resolve.
 */
@RestController
@RequestMapping("/api/v1/platform/pricing")
@RequiredArgsConstructor
public class PlatformPricingController {

    private final PlatformPricingService platformPricingService;

    @GetMapping
    public PlatformPricingResponse getPricing() {
        return platformPricingService.getPublicPricing();
    }
}
