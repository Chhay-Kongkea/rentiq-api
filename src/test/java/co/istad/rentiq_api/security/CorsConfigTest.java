package co.istad.rentiq_api.security;

import co.istad.rentiq_api.common.config.props.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backend audit CONF-001 — SecurityConfig's {@code .cors(Customizer.withDefaults())} only does
 * anything if a {@link CorsConfigurationSource} bean exists; without one, cross-origin requests
 * silently receive no Access-Control-Allow-* headers. Origins must come from configuration
 * (never hard-coded, never a wildcard) and credentials must only be allowed alongside explicit
 * origins.
 */
class CorsConfigTest {

    private CorsConfigurationSource sourceFor(List<String> allowedOrigins) {
        AppProperties properties = new AppProperties();
        properties.getCors().setAllowedOrigins(allowedOrigins);
        return new CorsConfig(properties).corsConfigurationSource();
    }

    @Test
    void corsConfigurationSource_usesConfiguredOrigins_neverAWildcard() {
        CorsConfigurationSource source = sourceFor(List.of("https://app.rentiq.example"));

        CorsConfiguration configuration = source.getCorsConfiguration(request());

        assertThat(configuration.getAllowedOrigins()).containsExactly("https://app.rentiq.example");
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void corsConfigurationSource_exposesExplicitMethodsAndHeaders() {
        CorsConfiguration configuration = sourceFor(List.of("http://localhost:3000"))
                .getCorsConfiguration(request());

        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
    }

    @Test
    void corsConfigurationSource_allowsCredentials_onlyAlongsideExplicitOrigins() {
        CorsConfiguration configuration = sourceFor(List.of("http://localhost:3000"))
                .getCorsConfiguration(request());

        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedOrigins()).isNotEmpty();
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void corsConfigurationSource_appliesToAllPaths() {
        CorsConfiguration configuration = sourceFor(List.of("http://localhost:3000"))
                .getCorsConfiguration(request("/api/v1/bookings"));

        assertThat(configuration).isNotNull();
    }

    private HttpServletRequest request() {
        return request("/api/v1/anything");
    }

    private HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
