package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;

public class CourseGroupRepositorySQLite implements CourseGroupRepository{

    private final CourseRepository courseRepository;

    private final JdbcTemplate jdbcTemplate;

    public CourseGroupRepositorySQLite(DataSource dataSource, CourseRepository courseRepository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.courseRepository = courseRepository;
    }

    @Override
    public List<CourseGroup> getByQuarters(int... quarters) {
        return jdbcTemplate.query(
                "SELECT * FROM course_groups",
                (ResultSet rs, int numRow) ->
                        new CourseGroup(
                                rs.getString("group_code"),
                                rs.getString("french_name"),
                                courseRepository.getByGroupIdAndQuarters(numRow, quarters)
                        )
        );
    }

}
