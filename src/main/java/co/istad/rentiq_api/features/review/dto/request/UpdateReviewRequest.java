package co.istad.rentiq_api.features.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @Min(1) @Max(5) Short rating,
        @Size(max = 2000) String reviewText
) {}