package co.istad.rentiq_api.features.review.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewImageInput(
        @NotBlank String imageUrl,
        String thumbnailUrl
) {}