package co.istad.rentiq_api.features.adminAudit.service;

import co.istad.rentiq_api.features.adminAudit.dto.response.AdminAuditLogResponse;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface AdminAuditService {

    /**
     * Resolves the authenticated admin internally (never trusts a caller-supplied admin id),
     * serializes oldValue/newValue as small, explicit snapshots (e.g. Map.of("status", ...)),
     * and persists one audit row. Call only after the business mutation has already succeeded.
     */
    void record(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            Object oldValue,
            Object newValue,
            String reason
    );

    Page<AdminAuditLogResponse> search(
            String adminId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    AdminAuditLogResponse getById(UUID id);
}
