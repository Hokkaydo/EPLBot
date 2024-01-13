package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.ExamsRetrieveThread;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

public class ExamRetrieveThreadRepositorySQLite implements ExamRetrieveThreadRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ExamsRetrieveThread> mapper = (ResultSet rs, int _) -> new ExamsRetrieveThread(rs.getLong("message_id"), rs.getString("path"));


    public ExamRetrieveThreadRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<ExamsRetrieveThread> readByMessageId(Long id) {
        List<ExamsRetrieveThread> l = jdbcTemplate.query("SELECT * FROM exams_thread WHERE message_id = ?", mapper, id);
        if(l.isEmpty()) return Optional.empty();
        return Optional.of(l.get(0));
    }

    @Override
    public void create(ExamsRetrieveThread... models) {
        for (ExamsRetrieveThread model : models) {
            jdbcTemplate.update("INSERT INTO exams_thread (message_id, path) VALUES (?, ?)", model.messageId(), model.path());
        }
    }

    @Override
    public List<ExamsRetrieveThread> readAll() {
        return jdbcTemplate.query("SELECT * FROM exams_thread", mapper);
    }

}
