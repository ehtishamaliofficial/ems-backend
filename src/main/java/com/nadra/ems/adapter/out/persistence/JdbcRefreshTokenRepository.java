package com.nadra.ems.adapter.out.persistence;

import com.nadra.ems.domain.model.RefreshToken;
import com.nadra.ems.domain.port.out.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * JDBC adapter implementing the {@link RefreshTokenRepository} driven port.
 */
@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRefreshTokenRepository.class);

    private static final RowMapper<RefreshToken> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        RefreshToken token = new RefreshToken();
        token.setId(rs.getLong("id"));
        token.setUserId(rs.getLong("user_id"));
        token.setTokenHash(rs.getString("token_hash"));

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        token.setExpiresAt(expiresAt != null ? expiresAt.toInstant() : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        token.setCreatedAt(createdAt != null ? createdAt.toInstant() : null);

        token.setRevoked(rs.getBoolean("revoked"));
        return token;
    };

    private final JdbcClient jdbcClient;

    public JdbcRefreshTokenRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(RefreshToken token) {
        log.debug("Saving refresh token for userId={}", token.getUserId());
        jdbcClient.sql("""
                        INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked)
                        VALUES (:userId, :tokenHash, :expiresAt, :revoked)
                        """)
                .param("userId", token.getUserId())
                .param("tokenHash", token.getTokenHash())
                .param("expiresAt", Timestamp.from(token.getExpiresAt()))
                .param("revoked", token.isRevoked())
                .update();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        log.debug("Finding refresh token by hash");
        try {
            RefreshToken token = jdbcClient.sql("SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash")
                    .param("tokenHash", tokenHash)
                    .query(ROW_MAPPER)
                    .single();
            return Optional.of(token);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revokeByUserId(Long userId) {
        log.info("Revoking all refresh tokens for userId={}", userId);
        jdbcClient.sql("UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = :userId AND revoked = FALSE")
                .param("userId", userId)
                .update();
    }

    @Override
    public void revokeByTokenHash(String tokenHash) {
        log.debug("Revoking refresh token by hash");
        jdbcClient.sql("UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .update();
    }

    @Override
    public int deleteExpired() {
        int deleted = jdbcClient.sql("DELETE FROM refresh_tokens WHERE expires_at < NOW() OR revoked = TRUE")
                .update();
        log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        return deleted;
    }
}
