package co.istad.rentiq_api.features.promotion.dto.request;

import co.istad.rentiq_api.features.promotion.enums.PromotionPackage;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePromotionRequest(

        @NotNull
        UUID itemId,

        @NotNull
        PromotionPackage packageType

) {}
