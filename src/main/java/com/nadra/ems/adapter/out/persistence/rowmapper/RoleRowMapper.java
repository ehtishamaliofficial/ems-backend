package com.nadra.ems.adapter.out.persistence.rowmapper;

import com.nadra.ems.domain.model.Role;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link Role} domain model.
 */
public class RoleRowMapper implements RowMapper<Role> {

    @Override
    public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("created_at");
        role.setCreatedAt(ts != null ? ts.toInstant() : null);

        return role;
    }
}
