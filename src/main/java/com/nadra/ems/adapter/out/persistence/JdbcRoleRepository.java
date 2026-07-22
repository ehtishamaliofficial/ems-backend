package com.nadra.ems.adapter.out.persistence;

import com.nadra.ems.adapter.out.persistence.rowmapper.RoleRowMapper;
import com.nadra.ems.domain.model.Role;
import com.nadra.ems.domain.port.out.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC adapter implementing the {@link RoleRepository} driven port.
 */
@Repository
public class JdbcRoleRepository implements RoleRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRoleRepository.class);
    private static final RoleRowMapper ROW_MAPPER = new RoleRowMapper();

    private final JdbcClient jdbcClient;

    public JdbcRoleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Role> findByName(String name) {
        log.debug("Finding role by name={}", name);
        try {
            Role role = jdbcClient.sql("SELECT * FROM roles WHERE name = :name")
                    .param("name", name)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(role);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Role> findAll() {
        return jdbcClient.sql("SELECT * FROM roles ORDER BY id")
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public List<Role> findRolesByUserId(Long userId) {
        log.debug("Finding roles for userId={}", userId);
        return jdbcClient.sql("""
                        SELECT r.* FROM roles r
                        INNER JOIN user_roles ur ON r.id = ur.role_id
                        WHERE ur.user_id = :userId
                        """)
                .param("userId", userId)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public void assignRoleToUser(Long userId, Integer roleId) {
        log.info("Assigning role: userId={}, roleId={}", userId, roleId);
        jdbcClient.sql("INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId) ON CONFLICT DO NOTHING")
                .param("userId", userId)
                .param("roleId", roleId)
                .update();
    }

    @Override
    public void removeRoleFromUser(Long userId, Integer roleId) {
        log.info("Removing role: userId={}, roleId={}", userId, roleId);
        jdbcClient.sql("DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId")
                .param("userId", userId)
                .param("roleId", roleId)
                .update();
    }
}
