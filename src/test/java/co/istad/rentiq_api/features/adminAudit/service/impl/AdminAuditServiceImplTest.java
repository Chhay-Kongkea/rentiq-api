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
import co.istad.rentiq_api.security.AuthUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceImplTest {

    @Mock private AdminAuditLogRepository auditLogRepository;
    @Mock private AdminAuditMapper auditMapper;

    private AdminAuditServiceImpl service;
    private MockedStatic<AuthUtils> authUtils;

    @BeforeEach
    void setUp() {
        // A real ObjectMapper (not mocked) so old/new value serialization is genuinely exercised.
        service = new AdminAuditServiceImpl(
                auditLogRepository, auditMapper, new ObjectMapper(), new AdminAuditPersistenceMapper()
        );
        authUtils = Mockito.mockStatic(AuthUtils.class);
        authUtils.when(AuthUtils::extractUserId).thenReturn("admin-1");
    }

    @AfterEach
    void tearDown() {
        authUtils.close();
    }

    @Test
    void record_resolvesAdminInternally_andPersistsSnapshotsAsPlainMaps() {
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(
                AdminAuditAction.USER_SUSPENDED,
                AdminAuditTargetType.USER,
                "user-42",
                Map.of("status", "ACTIVE"),
                Map.of("status", "SUSPENDED"),
                "Repeated policy violations");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminId()).isEqualTo("admin-1");
        assertThat(saved.getAction().name()).isEqualTo("USER_SUSPENDED");
        assertThat(saved.getTargetType().name()).isEqualTo("USER");
        assertThat(saved.getTargetId()).isEqualTo("user-42");
        assertThat(saved.getReason()).isEqualTo("Repeated policy violations");
        assertThat(saved.getOldValue()).isEqualTo(Map.of("status", "ACTIVE"));
        assertThat(saved.getNewValue()).containsAllEntriesOf(Map.of(
                "status", "SUSPENDED",
                "eventAction", "USER_SUSPENDED",
                "eventTargetType", "USER"
        ));
    }

    @Test
    void record_neverTrustsAnAdminIdFromTheCaller() {
        // The public record(...) signature has no adminId parameter at all — this test
        // documents that the identity always comes from AuthUtils, not from any argument.
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(AdminAuditAction.ITEM_APPROVED, AdminAuditTargetType.ITEM,
                UUID.randomUUID().toString(), null, Map.of("approvalStatus", "APPROVED"), null);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAdminId()).isEqualTo("admin-1");
    }

    @Test
    void record_allowsNullReasonAndNullOldValue() {
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(
                AdminAuditAction.MODERATION_ACTION_CREATED,
                AdminAuditTargetType.REPORT,
                UUID.randomUUID().toString(),
                null,
                Map.of("actionType", "WARNING"),
                null);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getReason()).isNull();
        assertThat(saved.getOldValue()).isNull();
        assertThat(saved.getNewValue()).containsAllEntriesOf(Map.of(
                "actionType", "WARNING",
                "eventAction", "MODERATION_ACTION_CREATED",
                "eventTargetType", "REPORT"
        ));
    }

    @Test
    void record_categoryCreate_usesLegacyCompatibleActionAndPreservesSemanticAction() {
        service.record(
                AdminAuditAction.CATEGORY_CREATED,
                AdminAuditTargetType.CATEGORY,
                UUID.randomUUID().toString(),
                null,
                Map.of("name", "Cameras"),
                null
        );

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction().name()).isEqualTo("MODERATION_ACTION_CREATED");
        assertThat(saved.getTargetType().name()).isEqualTo("CATEGORY");
        assertThat(saved.getNewValue()).containsEntry("eventAction", "CATEGORY_CREATED");
    }

    @Test
    void record_advertisementAction_mapsBothLegacyColumnsAndPreservesSemantics() {
        service.record(
                AdminAuditAction.ADVERTISEMENT_APPROVED,
                AdminAuditTargetType.ADVERTISEMENT,
                UUID.randomUUID().toString(), null, Map.of("status", "APPROVED"), null
        );

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAction().name()).isEqualTo("MODERATION_ACTION_CREATED");
        assertThat(saved.getTargetType().name()).isEqualTo("ITEM");
        assertThat(saved.getNewValue()).containsEntry("eventAction", "ADVERTISEMENT_APPROVED");
        assertThat(saved.getNewValue()).containsEntry("eventTargetType", "ADVERTISEMENT");
    }

    @Test
    void record_doesNotSwallowRepositoryFailure() {
        when(auditLogRepository.save(any())).thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.record(
                AdminAuditAction.CATEGORY_DELETED,
                AdminAuditTargetType.CATEGORY,
                UUID.randomUUID().toString(), null, null, null
        )).isInstanceOf(IllegalStateException.class).hasMessage("database failure");
    }

    @Test
    void getById_throwsNotFoundException_whenMissing() {
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void search_rejectsFromAfterTo() {
        assertThatThrownBy(() -> service.search(
                null, null, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), mock(Pageable.class)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void search_defaultsToRepositorySortOrder_newestFirstIsCallerResponsibility() {
        // The service delegates ordering to the Pageable the controller builds (which defaults
        // to createdAt DESC) — this test just proves the specification/page plumbing works.
        Pageable pageable = mock(Pageable.class);
        AdminAuditLog entity = AdminAuditLog.builder().id(UUID.randomUUID()).build();
        when(auditLogRepository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(auditMapper.toResponse(entity)).thenReturn(mock(AdminAuditLogResponse.class));

        Page<AdminAuditLogResponse> result = service.search(
                "admin-1", AdminAuditAction.USER_SUSPENDED, AdminAuditTargetType.USER, "user-42",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
