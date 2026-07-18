package co.istad.rentiq_api.features.offers.dto.request;

import java.math.BigDecimal;

public record UpdateOfferRequest(

        BigDecimal offeredPrice,

        String currency,

        String message

) {}