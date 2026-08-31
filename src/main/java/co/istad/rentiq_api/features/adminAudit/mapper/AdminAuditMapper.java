package co.istad.rentiq_api.features.adminAudit.mapper;

import co.istad.rentiq_api.features.adminAudit.dto.response.AdminAuditLogResponse;
import co.istad.rentiq_api.features.adminAudit.entity.AdminAuditLog;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminAuditMapper {

    default AdminAuditLogResponse toResponse(AdminAuditLog auditLog) {
        return new AdminAuditLogResponse(
                auditLog.getId(),
                auditLog.getAdminId(),
                semanticAction(auditLog),
                semanticTargetType(auditLog),
                auditLog.getTargetId(),
                auditLog.getReason(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getCreatedAt()
        );
    }

    private AdminAuditAction semanticAction(AdminAuditLog auditLog) {
        Object value = auditLog.getNewValue() == null ? null : auditLog.getNewValue().get("eventAction");
        return value == null
                ? AdminAuditAction.valueOf(auditLog.getAction().name())
                : AdminAuditAction.valueOf(value.toString());
    }

    private AdminAuditTargetType semanticTargetType(AdminAuditLog auditLog) {
        Object value = auditLog.getNewValue() == null ? null : auditLog.getNewValue().get("eventTargetType");
        return value == null
                ? AdminAuditTargetType.valueOf(auditLog.getTargetType().name())
                : AdminAuditTargetType.valueOf(value.toString());
    }
}
