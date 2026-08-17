package co.istad.rentiq_api.features.auth.service;

import co.istad.rentiq_api.features.auth.RoleEnum;

public interface KeycloakRoleService {

    boolean hasRealmRole(String userId, RoleEnum role);

    void assignRealmRole(String userId, RoleEnum role);
}
