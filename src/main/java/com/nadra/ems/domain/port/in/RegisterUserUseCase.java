package com.nadra.ems.domain.port.in;

import com.nadra.ems.domain.model.User;

/**
 * Driving port — use case for registering a new user/employee in the EMS.
 */
public interface RegisterUserUseCase {

    /**
     * Registers a new user with the given details.
     *
     * @param user         the user domain object (erpNo, username, email, password, etc.)
     * @param rawPassword  the plain-text password to be hashed
     * @param roleName     the initial role to assign (e.g. "EMPLOYEE")
     * @return the created user with generated ID
     */
    User register(User user, String rawPassword, String roleName);
}
