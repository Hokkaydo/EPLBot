package com.github.hokkaydo.eplbot.module.points.repository;

import com.github.hokkaydo.eplbot.module.points.model.Points;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class PointsRepositorySQLite implements PointsRepository {
    private final JdbcTemplate jdbcTemplate;

    private List<String> roles = new ArrayList<>();
    private static final RowMapper<Points> mapper = (ResultSet rs, int _) ->
            new Points(
                    rs.getString("username"),
                    rs.getInt("points"),
                    rs.getString("role"),
                    rs.getInt("day"),
                    rs.getInt("month")
            );

    public PointsRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.roles = jdbcTemplate.queryForStream(
                "SELECT DISTINCT role FROM points",
                (ResultSet rs, int _) -> rs.getString("role")
        ).toList();
    }



    @Override
    public void create(Points... points) {
        for (Points point : points) {
            jdbcTemplate.update("""
                    INSERT INTO points (
                        username,
                        points,
                        role,
                        day,
                        month
                        )
                    VALUES (?,?,?,?,?)
                    """, point.username(), point.points(), point.role(), point.day(), point.month());


    }}

    @Override
    public List<Points> readAll() {
        return jdbcTemplate.query("SELECT * FROM points", mapper);
    }

    @Override
    @Transactional
    public int get(String username) {

        List<Points> userPoints = jdbcTemplate.query(
                "SELECT * FROM points WHERE username = ?",
                mapper,
                username
        );

        if (userPoints.isEmpty()) {
            return 0;
        }

        return userPoints.get(0).points();
    }

    public Points getUser(String username) {
        List<Points> userPoints = jdbcTemplate.query(
                "SELECT * FROM points WHERE username = ?",
                mapper,
                username
        );

        if (userPoints.isEmpty()) {
            return null;
        }

        return userPoints.get(0);
    }

    public void updateDate(String username, int day, int month) {
        jdbcTemplate.update("""
                UPDATE points
                SET day = ?,
                month = ?
                WHERE username = ?
                """, day, month, username);
    }
    public void delete() {
        jdbcTemplate.update("DELETE FROM points");
    }

    public int getPointsOfRole(String role) {
        //Get the sum of points of all users with the role
        return jdbcTemplate.queryForStream(
                "SELECT SUM(points) FROM points WHERE role = ?",
                mapper,
                role
        ).toList().get(0).points();
    }
    public void update(String username, int points) {
        jdbcTemplate.update("""
                UPDATE points
                SET points = ?
                WHERE username = ?
                """, points, username);

    }
    public void resetAll() {
        jdbcTemplate.update("""
                UPDATE points
                SET points = 0
                """);
}
    public boolean dailyStatus(String username, int day, int month) {
        //Get day and month
        List<Points> userPoints = jdbcTemplate.query(
                "SELECT * FROM points WHERE username = ?",
                mapper,
                username
        );
        if (userPoints.isEmpty()) {
            System.out.println("User not found");
            return false;
        }
        if (userPoints.get(0).day() == day && userPoints.get(0).month() == month) {
            return true;
        }
        return false;
    }

    @Transactional
    public boolean checkPresence(Member author) {
        //Check if author is in the database
        List<Points> userPoints = jdbcTemplate.query(
                "SELECT * FROM points WHERE username = ?",
                mapper,
                author.getUser().getName()
        );
        if (userPoints.isEmpty()) {
            return false;
        }
        return true;
    }






}
