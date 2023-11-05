package com.github.hokkaydo.eplbot.configuration.repository;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;

public class ConfigurationRepositorySQLite implements ConfigurationRepository{

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ConfigurationModel> mapper = (rs, rowNum) -> new ConfigurationModel(
            rs.getString("key"),
            rs.getString("value"),
            rs.getLong("guild_id")
    );

    public ConfigurationRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void updateGuildVariable(Long guildId, String key, String value) {
        jdbcTemplate.update("INSERT INTO configuration (guild_id, key, value, state) VALUES (?,?,?,0)", guildId, key, value);
    }

    @Override
    public void updateGuildState(Long guildId, String key, String value) {
        jdbcTemplate.update("INSERT INTO configuration (guild_id, key, value, state) VALUES (?,?,?,1)", guildId, key, value);
    }

    @Override
    public List<ConfigurationModel> getGuildStates() {
        return jdbcTemplate.query("SELECT * FROM configuration WHERE state=1", mapper);
    }

    @Override
    public List<ConfigurationModel> getGuildVariables() {
        return jdbcTemplate.query("SELECT * FROM configuration WHERE state=0", mapper);
    }

}
