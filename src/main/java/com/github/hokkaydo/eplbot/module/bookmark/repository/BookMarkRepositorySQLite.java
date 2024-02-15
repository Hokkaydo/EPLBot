package com.github.hokkaydo.eplbot.module.bookmark.repository;

import com.github.hokkaydo.eplbot.module.bookmark.model.BookMark;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;

public class BookMarkRepositorySQLite implements BookMarkRepository {

    private static final RowMapper<BookMark> MAPPER = (rs, _) -> new BookMark(
            rs.getLong("user_id"),
            rs.getLong("message_id"),
            rs.getString("description"),
            rs.getString("message_link")
    );
    private final JdbcTemplate jdbcTemplate;

    public BookMarkRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(BookMark... models) {
        for (BookMark model : models) {
            jdbcTemplate.update("INSERT INTO bookmarks (user_id, message_id, description, message_link) VALUES (?,?,?,?)", model.userId(), model.messageId(), model.description(), model.messageLink());
        }
    }

    @Override
    public List<BookMark> readAll() {
        return jdbcTemplate.query("SELECT * FROM bookmarks",  MAPPER);
    }

    @Override
    public List<BookMark> getByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM bookmarks WHERE user_id=?", MAPPER, userId);
    }

    @Override
    public void deleteByMessageId(Long messageId) {
        jdbcTemplate.update("DELETE FROM bookmarks WHERE message_id=?", messageId);
    }

    @Override
    public void deleteByUserAndMessageId(Long userId, Long messageId) {
        jdbcTemplate.update("DELETE FROM bookmarks WHERE user_id=? AND message_id=?", userId, messageId);
    }

}
