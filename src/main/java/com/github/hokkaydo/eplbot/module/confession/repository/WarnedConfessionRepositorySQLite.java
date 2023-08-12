package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Stream;

public class WarnedConfessionRepositorySQLite implements WarnedConfessionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final static RowMapper<WarnedConfession> mapper = (ResultSet rs, int numRow) ->
            new WarnedConfession(
                rs.getLong("moderator_id"),
                rs.getLong("author_id"),
                rs.getString("message_content"),
                rs.getTimestamp("timestamp")
            );

    public WarnedConfessionRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(WarnedConfession warnedConfession) {
        jdbcTemplate.update("""
                INSERT INTO warned_confessions (
                    moderator_id,
                    author_id,
                    message_content,
                    timestamp
                    )
                VALUES (?,?,?,?)
                """, warnedConfession.moderatorId(), warnedConfession.authorId(), warnedConfession.messageContent(), warnedConfession.timestamp());
    }

    @Override
    public List<WarnedConfession> readByAuthor(long authorId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM warned_confessions WHERE author_id = ?",
                mapper,
                authorId
        ).toList();
    }

    @Override
    public void deleteByAuthor(long author) {
        jdbcTemplate.update("DELETE FROM warned_confessions WHERE author_id = ?", author);
    }
}
