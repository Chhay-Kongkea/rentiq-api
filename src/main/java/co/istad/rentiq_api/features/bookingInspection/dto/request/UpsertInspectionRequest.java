package co.istad.rentiq_api.features.bookingInspection.dto.request;

import jakarta.validation.constraints.Size;

public record UpsertInspectionRequest(
        @Size(max = 4000) String checkInNotes,
        @Size(max = 4000) String checkOutNotes
) {}