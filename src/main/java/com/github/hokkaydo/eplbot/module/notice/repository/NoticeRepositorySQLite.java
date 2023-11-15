package com.github.hokkaydo.eplbot.module.notice.repository;

import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepository;
import com.github.hokkaydo.eplbot.module.notice.model.Notice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoticeRepositorySQLite implements NoticeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Notice> courseMapper;
    private final RowMapper<Notice> groupMapper;

    public NoticeRepositorySQLite(CourseRepository courseRepository, CourseGroupRepository courseGroupRepository) {
        this.jdbcTemplate = new JdbcTemplate(DatabaseManager.getDataSource());
        this.courseMapper = (rs, rowNum) -> new Notice(
                rs.getString("content"),
                rs.getString("author_id"),
                courseRepository.getByCourseCode(rs.getString("subject_id")).orElse(null),
                null,
                rs.getTimestamp("timestamp")
        );
        this.groupMapper = (rs, rowNum) -> new Notice(
                rs.getString("content"),
                rs.getString("author_id"),
                null,
                courseGroupRepository.readByGroupCode(rs.getString("subject_id")).orElse(null),
                rs.getTimestamp("timestamp")
        );
    }

    @Override
    public void create(Notice... models) {
        for (Notice model : models) {
            jdbcTemplate.update("INSERT INTO notices(author_id, subject_id, content, timestamp, type) VALUES (?,?,?,?,?)",
                    model.authorId(),
                    model.course() == null ? model.courseGroup().groupCode() : model.course().code(),
                    model.content(),
                    model.timestamp(),
                    model.course() == null ? "group" : "course"
            );
        }
    }

    @Override
    public List<Notice> readAll() {
        List<Notice> result = new ArrayList<>();
        result.addAll(jdbcTemplate.query("SELECT * FROM notices WHERE type = ?", courseMapper, "course"));
        result.addAll(jdbcTemplate.query("SELECT * FROM notices WHERE type = ?", groupMapper, "group"));
        return result;
    }

    @Override
    public void update(Notice oldModel, Notice newModel) {
        jdbcTemplate.update(
                "UPDATE notices SET content = ?, timestamp = ? WHERE author_id = ? AND subject_id = ?",
                newModel.content(), newModel.timestamp(), newModel.authorId(), newModel.course() == null ? newModel.courseGroup().groupCode() : newModel.course().code());
    }

    @Override
    public Optional<Notice> readByAuthorIdAndSubjectId(String authorId, String subjectId, boolean isCourse) {
        return readAll().stream()
                       .filter(n -> n.authorId().equals(authorId))
                       .filter(n -> isCourse == (n.courseGroup() == null))
                       .filter(n -> (isCourse && n.course().code().equalsIgnoreCase(subjectId)) || (!isCourse && n.courseGroup().groupCode().equalsIgnoreCase(subjectId)))
                       .findFirst();
    }

    @Override
    public List<Notice> readBySubjectId(String subjectId, boolean isCourse) {
        return jdbcTemplate.query(
                "SELECT * FROM notices WHERE subject_id = ? AND type = ?",
                isCourse ? courseMapper : groupMapper,
                subjectId, isCourse ? "course" : "group"
        );
    }

}
