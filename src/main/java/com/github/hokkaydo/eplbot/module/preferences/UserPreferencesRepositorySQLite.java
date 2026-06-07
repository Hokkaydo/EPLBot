package com.github.hokkaydo.eplbot.module.preferences;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

public class UserPreferencesRepositorySQLite implements UserPreferencesRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserPreferencesRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<String> getPreference(long userId, long guildId, String key) {
        List<String> results = jdbcTemplate.queryForList(
                "SELECT value FROM user_preferences WHERE user_id=? AND guild_id=? AND preference_key=?",
                String.class, userId, guildId, key
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void setPreference(long userId, long guildId, String key, String value) {
        jdbcTemplate.update("""
                INSERT OR REPLACE INTO user_preferences (id, user_id, guild_id, preference_key, value)
                VALUES ((SELECT id FROM user_preferences WHERE user_id=? AND guild_id=? AND preference_key=?), ?, ?, ?, ?)
                """, userId, guildId, key, userId, guildId, key, value);
    }
}
