package co.istad.rentiq_api.features.bookingInspection.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InspectionImageInput(
        @NotBlank String imageName,
        @NotBlank @Pattern(regexp = "CHECK_IN|CHECK_OUT", message = "type must be CHECK_IN or CHECK_OUT") String type
) {}