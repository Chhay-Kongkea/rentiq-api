package co.istad.rentiq_api.features.adminUserManagement.controller;

import co.istad.rentiq_api.features.adminUserManagement.dto.request.AdminUserModerationRequest;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserResponse;
import co.istad.rentiq_api.features.adminUserManagement.dto.response.AdminUserStatusResponse;
import co.istad.rentiq_api.features.adminUserManagement.service.AdminUserManagementService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserManagementService adminUserManagementService;

    @GetMapping
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return adminUserManagementService.listUsers(search, pageable);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable String id) {
        return adminUserManagementService.getUser(id);
    }

    @PatchMapping("/{id}/suspend")
    public AdminUserStatusResponse suspendUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserModerationRequest request
    ) {
        return adminUserManagementService.suspendUser(id, request.reason(), AuthUtils.extractUserId());
    }

    @PatchMapping("/{id}/ban")
    public AdminUserStatusResponse banUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserModerationRequest request
    ) {
        return adminUserManagementService.banUser(id, request.reason(), AuthUtils.extractUserId());
    }

    @PatchMapping("/{id}/reinstate")
    public AdminUserStatusResponse reinstateUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserModerationRequest request
    ) {
        return adminUserManagementService.reinstateUser(id, request.reason(), AuthUtils.extractUserId());
    }
}
