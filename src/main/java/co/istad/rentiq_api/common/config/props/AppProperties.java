package co.istad.rentiq_api.common.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs both the OAuth2 login redirect (see SecurityConfig) and CORS allowed origins (see
 * CorsConfig) — a single environment-driven source of truth for "where the frontend lives",
 * instead of hard-coding a localhost origin in Java.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String frontendUrl;
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}
