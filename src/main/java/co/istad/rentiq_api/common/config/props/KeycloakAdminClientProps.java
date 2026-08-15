package co.istad.rentiq_api.common.config.props;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
@Slf4j
public class KeycloakAdminClientProps {

    private String serverUrl;
    private String clientId;
    private String clientSecret;
    private String realm;
    private String targetRealm;

    @PostConstruct
    public void checkConfig() {
        log.info("===== KEYCLOAK CONFIG =====");
        log.info("serverUrl   = {}", serverUrl);
        log.info("clientId    = {}", clientId);
        log.info("realm       = {}", realm);
        log.info("targetRealm = {}", targetRealm);

        // Never print the real secret
        log.info(
                "clientSecret loaded = {}, length = {}",
                clientSecret != null && !clientSecret.isBlank(),
                clientSecret == null ? 0 : clientSecret.length()
        );
    }
}