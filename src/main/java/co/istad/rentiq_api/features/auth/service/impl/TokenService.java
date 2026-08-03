package co.istad.rentiq_api.features.auth.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class TokenService {


    private final OAuth2AuthorizedClientService service;



    public String getAccessToken(
            OAuth2AuthenticationToken authentication
    ){


        OAuth2AuthorizedClient client =
                service.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName()
                );


        return client
                .getAccessToken()
                .getTokenValue();

    }

}