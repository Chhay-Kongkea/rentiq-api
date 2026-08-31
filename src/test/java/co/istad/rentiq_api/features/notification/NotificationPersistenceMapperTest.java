package co.istad.rentiq_api.features.notification;

import co.istad.rentiq_api.features.notification.entity.Notification;
import co.istad.rentiq_api.features.notification.enums.NotificationReferenceType;
import co.istad.rentiq_api.features.notification.enums.NotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPersistenceMapperTest {

    @Test
    void eventMappingMatchesLegacyDatabaseContractExactly() {
        Map<NotificationType, NotificationType> expected = Map.ofEntries(
                Map.entry(NotificationType.BOOKING, NotificationType.BOOKING),
                Map.entry(NotificationType.PAYMENT, NotificationType.PAYMENT),
                Map.entry(NotificationType.ITEM_REQUEST, NotificationType.ITEM_REQUEST),
                Map.entry(NotificationType.OFFER, NotificationType.OFFER),
                Map.entry(NotificationType.ITEM, NotificationType.ITEM),
                Map.entry(NotificationType.MARKETING, NotificationType.MARKETING),
                Map.entry(NotificationType.SYSTEM, NotificationType.SYSTEM),
                Map.entry(NotificationType.VENDOR_APPLICATION, NotificationType.SYSTEM),
                Map.entry(NotificationType.KYC, NotificationType.SYSTEM),
                Map.entry(NotificationType.ADVERTISEMENT, NotificationType.SYSTEM),
                Map.entry(NotificationType.PROMOTION, NotificationType.SYSTEM),
                Map.entry(NotificationType.DISPUTE, NotificationType.SYSTEM),
                Map.entry(NotificationType.REPORT, NotificationType.SYSTEM),
                Map.entry(NotificationType.REVIEW, NotificationType.SYSTEM));

        assertThat(expected).allSatisfy((semantic, persisted) ->
                assertThat(NotificationPersistenceMapper.resolveType(semantic)).isEqualTo(persisted));
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void everySemanticEventMapsToLegacySupportedPersistedType(NotificationType semanticType) {
        NotificationType persisted = NotificationPersistenceMapper.resolveType(semanticType);

        assertThat(persisted).isIn(
                NotificationType.BOOKING, NotificationType.PAYMENT, NotificationType.ITEM_REQUEST,
                NotificationType.OFFER, NotificationType.ITEM, NotificationType.MARKETING,
                NotificationType.SYSTEM);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType.class, names = {
            "VENDOR_APPLICATION", "KYC", "ADVERTISEMENT", "PROMOTION",
            "DISPUTE", "REPORT", "REVIEW"
    })
    void newerEventsPersistAsSystemAndRetainSemanticMetadata(NotificationType semanticType) {
        Notification notification = Notification.builder()
                .notificationType(semanticType)
                .title("Event approved")
                .build();

        NotificationPersistenceMapper.prepareForPersistence(notification);

        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(notification.getPayload()).containsEntry("eventType", semanticType.name());
    }

    @ParameterizedTest
    @EnumSource(NotificationReferenceType.class)
    void everyReferenceIsEitherLegacySupportedOrSafelyNull(NotificationReferenceType semanticType) {
        UUID referenceId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .notificationType(NotificationType.SYSTEM)
                .referenceType(semanticType)
                .referenceId(referenceId)
                .build();

        NotificationPersistenceMapper.prepareForPersistence(notification);

        assertThat(notification.getReferenceType()).satisfiesAnyOf(
                value -> assertThat(value).isNull(),
                value -> assertThat(NotificationPersistenceMapper.isPersistedReferenceType(value)).isTrue());
        assertThat(notification.getPayload()).containsEntry("referenceType", semanticType.name());
        assertThat(notification.getPayload()).containsEntry("referenceId", referenceId.toString());
    }
}
