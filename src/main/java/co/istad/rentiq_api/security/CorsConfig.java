package co.istad.rentiq_api.security;

import co.istad.rentiq_api.common.config.props.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig's {@code .cors(Customizer.withDefaults())} has nothing to draw from without a
 * {@link CorsConfigurationSource} bean (backend audit CONF-001) — cross-origin requests from
 * the configured frontend origin(s) would otherwise never receive the necessary
 * Access-Control-Allow-* headers. Origins come from {@code app.cors.allowed-origins}
 * (environment-driven — see {@link AppProperties}), never hard-coded and never a wildcard.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type", "Accept");

    private final AppProperties appProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        // Explicit configured origins only (never "*"), so allowing credentials here does not
        // create the wildcard-origin-plus-credentials combination browsers reject/flag unsafe.
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
