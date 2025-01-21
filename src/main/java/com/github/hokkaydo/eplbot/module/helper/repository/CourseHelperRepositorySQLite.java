package com.github.hokkaydo.eplbot.module.helper.repository;

import com.github.hokkaydo.eplbot.module.helper.model.CourseHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;

public class CourseHelperRepositorySQLite implements CourseHelperRepository {

    private static final RowMapper<CourseHelper> mapper = (ResultSet rs, int ignored) -> new CourseHelper(
            rs.getLong("channel_id"),
            rs.getLong("user_id"),
            rs.getLong("allows_ping") == 1,
            rs.getLong("is_tutor") == 1
    );

    private final JdbcTemplate jdbcTemplate;

    public CourseHelperRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(CourseHelper... courseHelpers) {
        for (CourseHelper courseHelper : courseHelpers) {
            jdbcTemplate.update("""
                INSERT INTO course_helpers (
                    channel_id,
                    user_id,
                    allows_ping,
                    is_tutor
                    )
                VALUES (?,?,?, ?)
                """, courseHelper.channelId(), courseHelper.userId(), courseHelper.allowsPing() ? 1 : 0, courseHelper.isTutor() ? 1 : 0);
        }
    }

    @Override
    public List<CourseHelper> readAll() {
        return jdbcTemplate.query("SELECT * FROM course_helpers", mapper);
    }

    @Override
    public void delete(CourseHelper courseHelper) {
        jdbcTemplate.update("DELETE FROM course_helpers WHERE channel_id = ? AND user_id = ?", courseHelper.channelId(), courseHelper.userId());
    }

    @Override
    public List<CourseHelper> readByChannelId(Long channelId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM course_helpers WHERE channel_id = ?",
                mapper,
                channelId
        ).toList();
    }

    @Override
    public List<CourseHelper> readByUserId(Long userId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM course_helpers WHERE user_id = ?",
                mapper,
                userId
        ).toList();
    }

    @Override
    public void deleteByChannelId(Long channelId) {
        jdbcTemplate.update("DELETE FROM course_helpers WHERE channel_id = ?", channelId);
    }

    @Override
    public void updatePing(Long channelId, Long userId, boolean allowPing) {
        jdbcTemplate.update("UPDATE course_helpers SET allows_ping = ? WHERE channel_id = ? AND user_id = ?", allowPing ? 1 : 0, channelId, userId);
    }

    @Override
    public void updateTutor(Long channelId, Long userId, boolean tutor) {
        jdbcTemplate.update("UPDATE course_helpers SET is_tutor = ? WHERE channel_id = ? AND user_id = ?", tutor ? 1 : 0, channelId, userId);
    }

}
