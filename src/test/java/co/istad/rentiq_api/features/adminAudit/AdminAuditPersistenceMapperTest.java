package co.istad.rentiq_api.features.adminAudit;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditPersistedTargetType;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuditPersistenceMapperTest {

    private final AdminAuditPersistenceMapper mapper = new AdminAuditPersistenceMapper();

    @ParameterizedTest
    @EnumSource(AdminAuditAction.class)
    void everySemanticActionMapsToAnExactDbSupportedAction(AdminAuditAction semanticAction) {
        AdminAuditPersistedAction persisted = mapper.resolveAction(semanticAction);

        assertThat(persisted).isIn(EnumSet.allOf(AdminAuditPersistedAction.class));
    }

    @ParameterizedTest
    @EnumSource(AdminAuditTargetType.class)
    void everySemanticTargetMapsToAnExactDbSupportedTarget(AdminAuditTargetType semanticTarget) {
        AdminAuditPersistedTargetType persisted = mapper.resolveTargetType(semanticTarget);

        assertThat(persisted).isIn(EnumSet.allOf(AdminAuditPersistedTargetType.class));
    }
}
