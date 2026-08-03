package co.istad.rentiq_api.security;

import co.istad.rentiq_api.common.exception.ForbiddenException;
import co.istad.rentiq_api.features.userProfile.entity.User;
import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import co.istad.rentiq_api.features.userProfile.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class
AccountStatusGuard {

    private final UserRepository userRepository;
    @Transactional
    public AccountStatus resolveStatus(String userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> createUser(userId))
                .getAccountStatus();
    }

    private User createUser(String userId) {
        try {
            return userRepository.saveAndFlush(User.builder().id(userId).build());
        } catch (DataIntegrityViolationException e) {

            return userRepository.findById(userId).orElseThrow(() -> e);
        }
    }

    public void assertNotSuspended(String userId) {
        if (resolveStatus(userId) == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account", "Your account is suspended and cannot perform this action");
        }
    }
}
