package co.istad.rentiq_api.security;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**",
                                "swagger-ui/**","swagger-ui.html").permitAll()
                        .requestMatchers(
                                "/scalar",
                                "/scalar/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        )
                        .permitAll()


                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/**","/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/category/**").permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/items",
                                        "/api/v1/items/featured",
                                        "/api/v1/items/nearby",
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/images",
                                        "/api/v1/items/*/reviews",
                                        "/api/v1/items/*/availability",
                                        "/api/v1/vendors/*/items"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/items",
                                        "/api/v1/items/*/images",
                                        "/api/v1/items/*/availability-block"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/availability",
                                        "/api/v1/items/*/status",
                                        "/api/v1/items/*/images/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/items/*",
                                        "/api/v1/items/*/images/*",
                                        "/api/v1/items/*/availability-block/*"
                                )
                                .hasRole("VENDOR")

                                .requestMatchers(
                                        "/api/v1/vendors/me/items",
                                        "/api/v1/vendors/me/items/**"
                                )
                                .hasRole("VENDOR")

                             .requestMatchers(
                                HttpMethod.GET,
                                  "/api/v1/items/*/images"
                             )
                             .permitAll()

                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/api/v1/items/*/images"
                            )
                            .hasRole("VENDOR")

                            .requestMatchers(
                                    HttpMethod.PATCH,
                                    "/api/v1/items/*/images/*"
                            )
                            .hasRole("VENDOR")

                            .requestMatchers(
                                    HttpMethod.DELETE,
                                    "/api/v1/items/*/images/*"
                            )
                            .hasRole("VENDOR")
                              .requestMatchers(
                                            "/api/v1/admin/items",
                                            "/api/v1/admin/items/**"
                                    )
                                    .hasRole("ADMIN")
                            .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {
        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = jwt -> {
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        };

        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

}
