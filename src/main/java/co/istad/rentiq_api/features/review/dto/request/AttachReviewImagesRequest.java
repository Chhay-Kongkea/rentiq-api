package co.istad.rentiq_api.features.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AttachReviewImagesRequest(
        @NotEmpty @Size(max = 10) List<@Valid ReviewImageInput> images
) {}