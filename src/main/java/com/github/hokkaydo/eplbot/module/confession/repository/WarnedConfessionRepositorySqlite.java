package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class WarnedConfessionRepositorySqlite implements WarnedConfessionRepository {
    @NonNull
    private DataSource dataSource;
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    private final RowMapper<WarnedConfession> mapper = (ResultSet rs, int numRow) ->
        new WarnedConfession(
                rs.getLong("id"),
                rs.getLong("moderator_id"),
                rs.getLong("author_id"),
                rs.getLong("message_id"),
                rs.getString("message_content"),
                rs.getTimestamp("timestamp")
        );

    @Override
    public Stream<WarnedConfession> readByAuthor(long authorId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM warned_confessions WHERE author_id = ?",
                mapper,
                authorId
        );
    }
}
