package com.github.hokkaydo.eplbot.module.translation.repository;

import com.github.hokkaydo.eplbot.module.translation.model.NameDescription;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class NameDescriptionRepositorySQLite implements NameDescriptionRepository {

    private final JdbcTemplate jdbcTemplate;
    public NameDescriptionRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(NameDescription... models) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO name_descriptions (
                    guild_id,
                    entity_id,
                    name,
                    description,
                    lang,
                    type
                ) VALUES (?,?,?,?,?,?)
                """, Arrays.asList(models), models.length, (ps, model) -> {
            ps.setLong(1, model.guildId());
            ps.setLong(2, model.id());
            ps.setString(3, model.name());
            ps.setString(4, model.description());
            ps.setString(5, model.lang());
            ps.setString(6, model.type().getName());
        });
    }

    @Override
    public List<NameDescription> readAll() {
        return jdbcTemplate.query("SELECT * FROM name_descriptions", (rs, ignored) -> new NameDescription(
                rs.getLong("guild_id"),
                rs.getLong("entity_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("lang"),
                NameDescription.Type.fromString(rs.getString("type"))
        ));
    }

    @Override
    public void deleteAllByGuildIdAndLang(Long guildId, String lang) {
        jdbcTemplate.update("DELETE FROM name_descriptions WHERE guild_id = ? AND lang = ?", guildId, lang);
    }

    @Override
    public List<NameDescription> readAllByGuildId(Long guildId) {
        return jdbcTemplate.query("SELECT * FROM name_descriptions WHERE guild_id = ?", (rs, ignored) -> new NameDescription(
                rs.getLong("guild_id"),
                rs.getLong("entity_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("lang"),
                NameDescription.Type.fromString(rs.getString("type"))
        ), guildId);
    }

    @Override
    public List<NameDescription> readAllByGuildIdAndLang(Long guildId, String lang) {
        return jdbcTemplate.query("SELECT * FROM name_descriptions WHERE guild_id = ? AND lang = ?", (rs, ignored) -> new NameDescription(
                rs.getLong("guild_id"),
                rs.getLong("entity_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("lang"),
                NameDescription.Type.fromString(rs.getString("type"))
        ), guildId, lang);
    }
}
