package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CourseRepositorySQLite implements CourseRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<Course> mapper = (rs, rowNum) -> new Course(
            rs.getString("course_code"),
            rs.getString("course_name"),
            rs.getInt("quarter"),
            rs.getInt("group_id")
    );

    public CourseRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<List<Course>> getByGroupIdAndQuarters(int id, int... quarters) {
        String ors = Arrays.stream(quarters).skip(1).mapToObj(i -> " OR quarter = ?").reduce((s1, s2) -> s1 + s2).orElse("");
        List<Course> list = jdbcTemplate.query(
                "SELECT (course_code, course_name, quarter, group_id) FROM courses WHERE group_id = ? AND (quarter = ? " + ors + ")",
                mapper,
                id, quarters[0]
        );
        List<List<Course>> ret = new ArrayList<>();

        for (int quarter : quarters) {
            List<Course> courses = new ArrayList<>();
            for (Course c : list) {
                if(c.quarter() == quarter)
                    courses.add(c);
            }
            ret.add(courses);
        }
        return ret;
    }

    @Override
    public List<List<Course>> getByGroupId(int id) {
        List<Course> list = jdbcTemplate.query(
                "SELECT (course_code, course_name, quarter, group_id) FROM courses WHERE group_id = ?",
                mapper,
                id
        );
        List<List<Course>> ret = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (Course course : list) {
            ret.get(course.quarter()).add(course);
        }
        return ret;
    }

    @Override
    public void create(Course model) {
        jdbcTemplate.update("INSERT INTO courses (course_code, course_name, quarter, group_id) VALUES(?,?,?,?)", model.code(), model.name(), model.quarter(), model.courseGroupId());
    }

    @Override
    public List<Course> readAll() {
        return jdbcTemplate.query("SELECT * FROM courses", mapper);
    }

}
