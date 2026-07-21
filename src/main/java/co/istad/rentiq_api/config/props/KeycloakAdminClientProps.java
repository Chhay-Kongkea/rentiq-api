package co.istad.rentiq_api.config.props;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakAdminClientProps {

    private String serverUrl;
    private String clientId;
    private String clientSecret;
    private String realm;
    private String targetRealm;



}
