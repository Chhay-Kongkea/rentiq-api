package co.istad.rentiq_api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Backend audit SEC-003 — AuthUtils must never blindly cast the current Authentication to
 * JwtAuthenticationToken; an unexpected principal type must fail cleanly (403), never surface
 * as an unhandled ClassCastException/500.
 */
class AuthUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", "jdoe")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void extractUserId_returnsJwtSubject_whenPrincipalIsJwt() {
        Jwt jwt = jwt("user-123");
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))));

        assertThat(AuthUtils.extractUserId()).isEqualTo("user-123");
    }

    @Test
    void extractUsername_readsPreferredUsernameClaim() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt("user-123"), List.of()));

        assertThat(AuthUtils.extractUsername()).isEqualTo("jdoe");
    }

    @Test
    void extractUserId_rejectsNonJwtPrincipal_insteadOfThrowingClassCastException() {
        // A non-JWT Authentication (e.g. the browser OAuth2 login flow's principal) must be
        // rejected explicitly via instanceof, never blindly cast.
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(AuthUtils::extractUserId)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void extractUserId_rejectsArbitraryAuthenticationType_insteadOfThrowingClassCastException() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("principal", "credentials"));

        assertThatThrownBy(AuthUtils::extractUserId)
                .isInstanceOf(ResponseStatusException.class)
                .isNotInstanceOf(ClassCastException.class);
    }

    @Test
    void extractUserId_rejectsWhenNoAuthenticationIsSet() {
        assertThatThrownBy(AuthUtils::extractUserId)
                .isInstanceOf(ResponseStatusException.class);
    }
}
