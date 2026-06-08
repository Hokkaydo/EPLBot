package com.github.hokkaydo.eplbot.module.remindme.repository;

import com.github.hokkaydo.eplbot.module.remindme.model.Reminder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public class ReminderRepositorySQLite implements ReminderRepository {

    private static final RowMapper<Reminder> MAPPER = (rs, ignored) -> new Reminder(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("guild_id"),
            rs.getLong("trigger_time"),
            rs.getString("subject")
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertWithKey;

    public ReminderRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.insertWithKey = new SimpleJdbcInsert(dataSource)
                .withTableName("reminders")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public void create(Reminder... models) {
        for (Reminder model : models) {
            createAndGetId(model);
        }
    }

    @Override
    public long createAndGetId(Reminder reminder) {
        Number key = insertWithKey.executeAndReturnKey(Map.of(
                "user_id", reminder.userId(),
                "guild_id", reminder.guildId(),
                "trigger_time", reminder.triggerTime(),
                "subject", reminder.subject()
        ));
        return key.longValue();
    }

    @Override
    public List<Reminder> readAll() {
        return jdbcTemplate.query("SELECT * FROM reminders", MAPPER);
    }

    @Override
    public List<Reminder> getByGuildId(long guildId) {
        return jdbcTemplate.query("SELECT * FROM reminders WHERE guild_id=?", MAPPER, guildId);
    }

    @Override
    public void deleteById(long id) {
        jdbcTemplate.update("DELETE FROM reminders WHERE id=?", id);
    }

}
