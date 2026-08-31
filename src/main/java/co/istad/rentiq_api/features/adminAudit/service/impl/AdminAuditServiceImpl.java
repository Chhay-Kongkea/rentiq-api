package co.istad.rentiq_api.features.adminAudit.service.impl;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.common.exception.NotFoundException;
import co.istad.rentiq_api.features.adminAudit.dto.response.AdminAuditLogResponse;
import co.istad.rentiq_api.features.adminAudit.AdminAuditPersistenceMapper;
import co.istad.rentiq_api.features.adminAudit.entity.AdminAuditLog;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.mapper.AdminAuditMapper;
import co.istad.rentiq_api.features.adminAudit.repository.AdminAuditLogRepository;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.adminAudit.specification.AdminAuditLogSpecification;
import co.istad.rentiq_api.security.AuthUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final ZoneOffset REPORTING_ZONE = ZoneOffset.UTC;
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};

    private final AdminAuditLogRepository auditLogRepository;
    private final AdminAuditMapper auditMapper;
    private final ObjectMapper objectMapper;
    private final AdminAuditPersistenceMapper persistenceMapper;

    @Override
    @Transactional
    public void record(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            Object oldValue,
            Object newValue,
            String reason
    ) {
        String adminId = AuthUtils.extractUserId();

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminId(adminId)
                .action(persistenceMapper.resolveAction(action))
                .targetType(persistenceMapper.resolveTargetType(targetType))
                .targetId(targetId)
                .reason(reason)
                .oldValue(toSnapshot(oldValue))
                .newValue(withSemanticMetadata(toSnapshot(newValue), action, targetType))
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> search(
            String adminId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidOperationException("from date must not be after to date");
        }

        OffsetDateTime fromInclusive = from == null ? null : from.atStartOfDay(REPORTING_ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime();

        return auditLogRepository
                .findAll(
                        AdminAuditLogSpecification.filter(
                                adminId,
                                action == null ? null : persistenceMapper.resolveAction(action),
                                targetType == null ? null : persistenceMapper.resolveTargetType(targetType),
                                targetId,
                                fromInclusive,
                                toExclusive),
                        pageable)
                .map(auditMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuditLogResponse getById(UUID id) {
        return auditLogRepository.findById(id)
                .map(auditMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Admin audit log", id));
    }

    /**
     * Converts an explicit snapshot (e.g. Map.of("status", ...)) into the plain Map the entity
     * stores as jsonb. Never pass full JPA entities here — see AdminAuditService's javadoc.
     * Falls back to an empty snapshot rather than letting a serialization problem roll back the
     * business mutation the caller already completed.
     */
    private Map<String, Object> toSnapshot(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, SNAPSHOT_TYPE);
        } catch (RuntimeException e) {
            log.warn("Failed to serialize audit snapshot of type {}, storing an empty snapshot instead",
                    value.getClass(), e);
            return Map.of();
        }
    }

    private Map<String, Object> withSemanticMetadata(
            Map<String, Object> snapshot,
            AdminAuditAction action,
            AdminAuditTargetType targetType
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (snapshot != null) {
            result.putAll(snapshot);
        }
        result.put("eventAction", action.name());
        result.put("eventTargetType", targetType.name());
        return result;
    }
}
