package com.github.hokkaydo.eplbot.configuration.repository;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationRepositorySQLite implements ConfigurationRepository{

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ConfigurationModel> mapper = (rs, rowNum) -> new ConfigurationModel(
            rs.getString("key"),
            rs.getString("value"),
            rs.getLong("guild_id"),
            rs.getInt("state")
    );

    public ConfigurationRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void updateGuildVariable(Long guildId, String key, String value) {
        jdbcTemplate.update("""
                                     INSERT OR REPLACE INTO configuration (id, guild_id, key, value, state)
                                     VALUES ((SELECT id FROM configuration WHERE guild_id=? AND key=? AND state=0), ?,?,?,0)
                                     """,
                guildId, key, guildId, key, value);
    }

    @Override
    public void updateGuildState(Long guildId, String key, String value) {
        jdbcTemplate.update("""
                                     INSERT OR REPLACE INTO configuration (id, guild_id, key, value, state)
                                     VALUES ((SELECT id FROM configuration WHERE guild_id=? AND key=? AND state=1), ?,?,?,1)
                                     """,
                guildId, key, guildId, key, value);
    }

    @Override
    public List<ConfigurationModel> getGuildStates() {
        return jdbcTemplate.query("SELECT * FROM configuration WHERE state=1", mapper);
    }

    @Override
    public List<ConfigurationModel> getGuildVariables() {
        return jdbcTemplate.query("SELECT * FROM configuration WHERE state=0", mapper);
    }

    @Override
    public void create(ConfigurationModel... models) {
        for (ConfigurationModel model : models) {
            jdbcTemplate.update("INSERT INTO configuration (guild_id, key, value, state) VALUES (?,?,?,?)", model.guildId(), model.key(), model.value(), model.state());
        }
    }

    @Override
    public List<ConfigurationModel> readAll() {
        List<ConfigurationModel> ret = new ArrayList<>();
        ret.addAll(getGuildStates());
        ret.addAll(getGuildVariables());
        return ret;
    }

}
