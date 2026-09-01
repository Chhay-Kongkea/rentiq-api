package co.istad.rentiq_api.features.platformSetting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePlatformSettingRequest(

        @NotNull
        Object value,

        @NotBlank
        @Size(max = 1000)
        String reason

) {}
