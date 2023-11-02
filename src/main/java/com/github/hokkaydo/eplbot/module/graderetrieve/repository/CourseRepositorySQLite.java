package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CourseRepositorySQLite implements CourseRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<Map.Entry<Integer, Course>> mapper = (rs, rowNum) -> Map.entry(
            rs.getInt("group_id"),
            new Course(rs.getString("course_code"), rs.getString("course_name"))
    );

    public CourseRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<List<Course>> getByGroupIdAndQuarters(int id, int... quarters) {
        String ors = Arrays.stream(quarters).skip(1).mapToObj(i -> " OR quarter = ?").reduce((s1, s2) -> s1 + s2).orElse("");
        List<Map.Entry<Integer, Course>> list = jdbcTemplate.query(
                "SELECT (course_code, course_name, group_id) FROM courses WHERE group_id = ? AND (quarter = ? " + ors + ")",
                mapper,
                id, quarters
        );
        List<List<Course>> ret = new ArrayList<>();
        for (int quarter : quarters) {
            List<Course> courses = new ArrayList<>();
            for (Map.Entry<Integer, Course> e : list) {
                if(e.getKey() == quarter)
                    courses.add(e.getValue());
            }
            ret.add(courses);
        }
        return ret;
    }

}
