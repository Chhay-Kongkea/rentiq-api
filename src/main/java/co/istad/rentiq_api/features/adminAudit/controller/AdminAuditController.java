package co.istad.rentiq_api.features.adminAudit.controller;

import co.istad.rentiq_api.features.adminAudit.dto.response.AdminAuditLogResponse;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only. Audit rows are written exclusively by AdminAuditService from within the business
 * services whose mutations they describe — there is deliberately no create/update/delete
 * endpoint here.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public Page<AdminAuditLogResponse> list(
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) AdminAuditAction action,
            @RequestParam(required = false) AdminAuditTargetType targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminAuditService.search(adminId, action, targetType, targetId, from, to, pageable);
    }

    @GetMapping("/{id}")
    public AdminAuditLogResponse getById(@PathVariable UUID id) {
        return adminAuditService.getById(id);
    }
}
