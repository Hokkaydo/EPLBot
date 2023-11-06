package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
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
                                rs.getInt("id"),
                                rs.getString("group_code"),
                                rs.getString("french_name"),
                                courseRepository.getByGroupIdAndQuarters(numRow, quarters)
                        )
        );
    }

    @Override
    public void create(CourseGroup model) {
        int id = jdbcTemplate.update("INSERT INTO course_groups (id, group_code, french_name) VALUES (?,?)", model.englishName(), model.frenchName());
        for (List<Course> l : model.courses()) {
            for (Course course : l) {
                courseRepository.create(new Course(course.code(), course.name(), course.quarter(), id));
            }
        }
    }

    @Override
    public List<CourseGroup> readAll() {
        return jdbcTemplate.query("SELECT * FROM course_groups", (ResultSet rs, int numRow) ->
                                                                         new CourseGroup(
                                                                                 rs.getInt("id"),
                                                                                 rs.getString("group_code"),
                                                                                 rs.getString("french_name"),
                                                                                 courseRepository.getByGroupId(numRow)
                                                                         ));
    }

}
