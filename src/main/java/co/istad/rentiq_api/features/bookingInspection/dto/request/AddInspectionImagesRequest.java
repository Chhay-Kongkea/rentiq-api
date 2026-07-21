package co.istad.rentiq_api.features.bookingInspection.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddInspectionImagesRequest(
        @NotEmpty @Size(max = 20) List<@Valid InspectionImageInput> images
) {}