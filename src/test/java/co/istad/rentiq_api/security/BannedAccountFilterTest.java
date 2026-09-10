package co.istad.rentiq_api.security;

import co.istad.rentiq_api.features.userProfile.enums.AccountStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Backend audit P0-1 — suspended/banned accounts must be rejected on every authenticated
 * request. This filter is the single, centrally-wired enforcement point (added after
 * BearerTokenAuthenticationFilter in SecurityConfig), so controllers need no per-endpoint checks.
 */
class BannedAccountFilterTest {

    private final AccountStatusGuard accountStatusGuard = mock(AccountStatusGuard.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final BannedAccountFilter filter = new BannedAccountFilter(accountStatusGuard, objectMapper);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private void authenticateAs(String userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(userId), List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void activeUser_isAllowedThrough() throws Exception {
        authenticateAs("user-1", "USER");
        when(accountStatusGuard.resolveStatus("user-1")).thenReturn(AccountStatus.ACTIVE);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void suspendedUser_isRejectedWith403_andChainNeverInvoked() throws Exception {
        authenticateAs("user-2", "USER");
        when(accountStatusGuard.resolveStatus("user-2")).thenReturn(AccountStatus.SUSPENDED);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void bannedUser_isRejectedWith403_andChainNeverInvoked() throws Exception {
        authenticateAs("user-3", "VENDOR");
        when(accountStatusGuard.resolveStatus("user-3")).thenReturn(AccountStatus.BANNED);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vendors/me/items");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("ACCOUNT_BANNED");
    }

    @Test
    void suspendedAdmin_isRejected_adminRoleDoesNotBypassAccountStatus() throws Exception {
        authenticateAs("admin-1", "ADMIN");
        when(accountStatusGuard.resolveStatus("admin-1")).thenReturn(AccountStatus.SUSPENDED);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void activeAdmin_isAllowedThrough() throws Exception {
        authenticateAs("admin-2", "ADMIN");
        when(accountStatusGuard.resolveStatus("admin-2")).thenReturn(AccountStatus.ACTIVE);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void resolveStatus_isQueriedAtMostOncePerRequest() throws Exception {
        authenticateAs("user-4", "USER");
        when(accountStatusGuard.resolveStatus("user-4")).thenReturn(AccountStatus.BANNED);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(accountStatusGuard, times(1)).resolveStatus(eq("user-4"));
    }

    @Test
    void publicEndpoint_withNoJwtAuthentication_isNotBlocked_andGuardIsNeverQueried() throws Exception {
        // Unauthenticated/public requests resolve to an anonymous Authentication, never a
        // JwtAuthenticationToken, so this filter must be a no-op and never hit the database.
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/items");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(accountStatusGuard);
    }

    @Test
    void missingAuthentication_isHandledSafely_withoutThrowing() throws Exception {
        // No SecurityContext authentication set at all (e.g. filter ordering edge case).
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/items");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(accountStatusGuard);
    }
}
