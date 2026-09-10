package co.istad.rentiq_api.features.adminAudit.service;

import co.istad.rentiq_api.features.adminAudit.dto.response.AdminAuditLogResponse;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface AdminAuditService {
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
