package co.istad.rentiq_api.features.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorReplyRequest(
        @NotBlank @Size(max = 2000) String reply
) {}