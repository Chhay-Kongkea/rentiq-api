package co.istad.rentiq_api.security;

import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Backend audit P0-1 — account status must always be resolved from the users table
 * (the source of truth), never trusted from a JWT claim.
 */
class AccountStatusGuardTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountStatusGuard guard = new AccountStatusGuard(userRepository);

    @Test
    void resolveStatus_returnsPersistedStatus_forActiveUser() {
        when(userRepository.findById("user-1"))
                .thenReturn(Optional.of(User.builder().id("user-1").accountStatus(AccountStatus.ACTIVE).build()));

        assertThat(guard.resolveStatus("user-1")).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void resolveStatus_returnsPersistedStatus_forSuspendedUser() {
        when(userRepository.findById("user-2"))
                .thenReturn(Optional.of(User.builder().id("user-2").accountStatus(AccountStatus.SUSPENDED).build()));

        assertThat(guard.resolveStatus("user-2")).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void resolveStatus_returnsPersistedStatus_forBannedUser() {
        when(userRepository.findById("user-3"))
                .thenReturn(Optional.of(User.builder().id("user-3").accountStatus(AccountStatus.BANNED).build()));

        assertThat(guard.resolveStatus("user-3")).isEqualTo(AccountStatus.BANNED);
    }

    @Test
    void resolveStatus_autoProvisionsMissingUser_asActive_insteadOfFailing() {
        when(userRepository.findById("new-user")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
                .thenReturn(User.builder().id("new-user").accountStatus(AccountStatus.ACTIVE).build());

        assertThat(guard.resolveStatus("new-user")).isEqualTo(AccountStatus.ACTIVE);
    }
}
