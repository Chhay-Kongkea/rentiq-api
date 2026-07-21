package co.istad.rentiq_api.features.notification.mapper;

import co.istad.rentiq_api.features.notification.dto.request.BroadcastNotificationRequest;
import co.istad.rentiq_api.features.notification.dto.response.NotificationResponse;
import co.istad.rentiq_api.features.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(
            target = "notificationType",
            source = "request.notificationType"
    )
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "body", source = "request.body")
    @Mapping(
            target = "payload",
            source = "request.payload",
            qualifiedByName = "copyPayload"
    )
    @Mapping(
            target = "referenceId",
            source = "request.referenceId"
    )
    @Mapping(
            target = "referenceType",
            source = "request.referenceType"
    )
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(
            BroadcastNotificationRequest request,
            String userId
    );

    NotificationResponse toResponse(
            Notification notification
    );

    @Named("copyPayload")
    default Map<String, Object> copyPayload(
            Map<String, Object> payload
    ) {
        if (payload == null) {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>(payload);
    }
}