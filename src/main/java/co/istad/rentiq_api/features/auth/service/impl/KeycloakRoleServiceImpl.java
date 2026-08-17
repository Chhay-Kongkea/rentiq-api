package co.istad.rentiq_api.features.auth.service.impl;

import co.istad.rentiq_api.common.config.props.KeycloakAdminClientProps;
import co.istad.rentiq_api.features.auth.RoleEnum;
import co.istad.rentiq_api.features.auth.exception.KeycloakOperationException;
import co.istad.rentiq_api.features.auth.service.KeycloakRoleService;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakRoleServiceImpl implements KeycloakRoleService {

    private final Keycloak keycloak;
    private final KeycloakAdminClientProps properties;

    @Override
    public boolean hasRealmRole(String userId, RoleEnum role) {
        try {
            return userResource(userId)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .anyMatch(role.name()::equalsIgnoreCase);
        } catch (RuntimeException exception) {
            throw new KeycloakOperationException(
                    "Failed to read roles for user " + userId,
                    exception
            );
        }
    }

    @Override
    public void assignRealmRole(String userId, RoleEnum role) {
        try {
            UserResource userResource = userResource(userId);

            boolean alreadyAssigned = userResource
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .anyMatch(role.name()::equalsIgnoreCase);

            if (alreadyAssigned) {
                return;
            }

            RealmResource realm = realmResource();
            RoleRepresentation roleRepresentation = realm
                    .roles()
                    .get(role.name())
                    .toRepresentation();

            userResource
                    .roles()
                    .realmLevel()
                    .add(List.of(roleRepresentation));

        } catch (RuntimeException exception) {
            throw new KeycloakOperationException(
                    "Failed to assign role " + role.name() + " to user " + userId,
                    exception
            );
        }
    }

    private RealmResource realmResource() {
        return keycloak.realm(properties.getTargetRealm());
    }

    private UserResource userResource(String userId) {
        return realmResource().users().get(userId);
    }
}
