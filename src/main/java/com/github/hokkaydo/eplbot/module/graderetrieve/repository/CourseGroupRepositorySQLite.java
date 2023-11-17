package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CourseGroupRepositorySQLite implements CourseGroupRepository{

    private final CourseRepository courseRepository;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;

    public CourseGroupRepositorySQLite(DataSource dataSource, CourseRepository courseRepository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.courseRepository = courseRepository;
        this.insert = new SimpleJdbcInsert(jdbcTemplate).withTableName("course_groups").usingGeneratedKeyColumns("id");
    }

    @Override
    public List<CourseGroup> readByQuarters(int... quarters) {
        return jdbcTemplate.query(
                "SELECT * FROM course_groups",
                (ResultSet rs, int numRow) ->
                        new CourseGroup(
                                rs.getInt("id"),
                                rs.getString("group_code"),
                                rs.getString("french_name"),
                                courseRepository.getByGroupIdAndQuarters(numRow + 1, quarters)
                        )
        );
    }

    @Override
    public Optional<CourseGroup> readByGroupCode(String groupCode) {
        return readAll().stream().filter(g -> g.groupCode().equalsIgnoreCase(groupCode)).findFirst();
    }

    @Override
    public void create(CourseGroup... models) {
        for (CourseGroup model : models) {
            Number id = insert.executeAndReturnKey(
                    Map.of(
                            "group_code", model.groupCode(),
                            "french_name", model.frenchName()
                    )
            );
            for (List<Course> l : model.courses()) {
                for (Course course : l) {
                    courseRepository.create(new Course(course.code(), course.name(), course.quarter(), id.intValue()));
                }
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
                                                                                 courseRepository.readByGroupId(numRow + 1)
                                                                         )
        );
    }

}
