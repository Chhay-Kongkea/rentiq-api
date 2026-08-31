package co.istad.rentiq_api.features.notification.dto.request;

import co.istad.rentiq_api.features.notification.enums.NotificationType;
import co.istad.rentiq_api.features.notification.enums.BroadcastAudienceType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class BroadcastNotificationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void titleLengthsOneAndTwentyAreAccepted() {
        assertThat(validator.validate(request("a"))).isEmpty();
        assertThat(validator.validate(request("a".repeat(20)))).isEmpty();
    }

    @Test
    void titleLengthTwentyOneAndBlankAreRejected() {
        assertThat(validator.validate(request("a".repeat(21)))).isNotEmpty();
        assertThat(validator.validate(request(" "))).isNotEmpty();
    }

    @Test
    void unsupportedBusinessEventCannotBeUsedAsBroadcastPersistenceType() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                BroadcastAudienceType.SINGLE_USER, "user-1", NotificationType.KYC,
                "KYC update", "Body", null, null, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    private BroadcastNotificationRequest request(String title) {
        return new BroadcastNotificationRequest(
                BroadcastAudienceType.SINGLE_USER, "user-1", NotificationType.SYSTEM,
                title, "Body", null, null, null);
    }

    @Test
    void singleUserRequiresUserIdWhileAllUsersDoesNot() {
        BroadcastNotificationRequest invalidSingle = new BroadcastNotificationRequest(
                BroadcastAudienceType.SINGLE_USER, null, NotificationType.SYSTEM,
                "System notice", "Body", null, null, null);
        BroadcastNotificationRequest validAll = new BroadcastNotificationRequest(
                BroadcastAudienceType.ALL_USERS, null, NotificationType.SYSTEM,
                "System notice", "Body", null, null, null);

        assertThat(validator.validate(invalidSingle)).isNotEmpty();
        assertThat(validator.validate(validAll)).isEmpty();
    }

    @Test
    void audienceTypeIsRequired() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                null, null, NotificationType.SYSTEM, "System notice", "Body", null, null, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void blankBodyIsRejectedAndMarketingIsAccepted() {
        BroadcastNotificationRequest blankBody = new BroadcastNotificationRequest(
                BroadcastAudienceType.ALL_USERS, null, NotificationType.MARKETING,
                "New campaign", " ", null, null, null);
        BroadcastNotificationRequest marketing = new BroadcastNotificationRequest(
                BroadcastAudienceType.ALL_USERS, null, NotificationType.MARKETING,
                "New campaign", "Campaign details", null, null, null);

        assertThat(validator.validate(blankBody)).isNotEmpty();
        assertThat(validator.validate(marketing)).isEmpty();
    }
}
