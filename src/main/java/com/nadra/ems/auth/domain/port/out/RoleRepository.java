package com.nadra.ems.auth.domain.port.out;

import com.nadra.ems.auth.domain.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (SPI) — repository contract for Role persistence.
 */
public interface RoleRepository {

    /**
     * Finds a role by its name (e.g. "SUPER_ADMIN").
     */
    Optional<Role> findByName(String name);

    /**
     * Returns all available roles.
     */
    List<Role> findAll();

    /**
     * Returns all roles assigned to a specific user.
     */
    List<Role> findRolesByUserId(Long userId);

    /**
     * Assigns a role to a user.
     */
    void assignRoleToUser(Long userId, Integer roleId);

    /**
     * Removes a role from a user.
     */
    void removeRoleFromUser(Long userId, Integer roleId);
}
