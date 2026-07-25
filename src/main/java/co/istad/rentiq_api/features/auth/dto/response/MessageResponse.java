package co.istad.rentiq_api.features.auth.dto.response;

import lombok.Builder;

@Builder
public record MessageResponse(String message) {}