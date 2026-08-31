package co.istad.rentiq_api.features.adminAudit.dto.response;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        String adminId,
        AdminAuditAction action,
        AdminAuditTargetType targetType,
        String targetId,
        String reason,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        OffsetDateTime createdAt
) {}
