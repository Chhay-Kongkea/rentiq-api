package co.istad.rentiq_api.security;


import co.istad.rentiq_api.config.props.KeycloakAdminClientProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KeyCloakAdminConfig {

    private final KeycloakAdminClientProps props;

    @Bean
    public Keycloak configKeycloak(){
        return KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getTargetRealm())
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecrete())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();


    }
}
