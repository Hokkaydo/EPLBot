package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.stream.Stream;

public class WarnedConfessionRepositorySQLite implements WarnedConfessionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<WarnedConfession> mapper = (ResultSet rs, int numRow) ->
            new WarnedConfession(
                    rs.getLong("id"),
                    rs.getLong("moderator_id"),
                    rs.getLong("author_id"),
                    rs.getLong("message_id"),
                    rs.getString("message_content"),
                    rs.getTimestamp("timestamp")
            );

    public WarnedConfessionRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Stream<WarnedConfession> readByAuthor(long authorId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM warned_confessions WHERE author_id = ?",
                mapper,
                authorId
        );
    }
}
