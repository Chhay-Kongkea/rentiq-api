package co.istad.rentiq_api.features.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationAuthorizationTest {

    @Test
    void inboxAllowsUserAndVendorRoles() {
        PreAuthorize annotation = NotificationController.class.getAnnotation(PreAuthorize.class);
        assertThat(annotation.value()).isEqualTo("hasAnyRole('USER', 'VENDOR')");
    }

    @Test
    void adminNotificationApisRemainAdminOnly() {
        PreAuthorize annotation = AdminNotificationController.class.getAnnotation(PreAuthorize.class);
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
