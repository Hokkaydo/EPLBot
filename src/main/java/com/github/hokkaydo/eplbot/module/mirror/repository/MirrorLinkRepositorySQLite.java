package com.github.hokkaydo.eplbot.module.mirror.repository;

import com.github.hokkaydo.eplbot.module.mirror.model.MirrorLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

public class MirrorLinkRepositorySQLite implements MirrorLinkRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<MirrorLink> mapper = (ResultSet rs, int _) -> new MirrorLink(rs.getLong("first_id"), rs.getLong("second_id"));

    public MirrorLinkRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(MirrorLink... mirrorLinks) {
        for (MirrorLink mirrorLink : mirrorLinks) {
            jdbcTemplate.update("""
                INSERT INTO mirrors (
                    first_id,
                    second_id
                    )
                VALUES (?,?)
                """, mirrorLink.first().getIdLong(), mirrorLink.second().getIdLong());
        }
    }

    @Override
    public List<MirrorLink> readAll() {
        return jdbcTemplate.query("SELECT * FROM mirrors", mapper);
    }

    @Override
    public List<MirrorLink> readyById(Long channelId) {
        return jdbcTemplate.queryForStream(
                "SELECT * FROM mirrors WHERE first_id = ? OR second_id = ?",
                mapper,
                channelId, channelId
        ).toList();
    }


    @Override
    public void deleteByIds(Long idA, Long idB) {
        jdbcTemplate.update("DELETE FROM mirrors WHERE first_id = ? AND second_id = ?", idA, idB);
        jdbcTemplate.update("DELETE FROM mirrors WHERE first_id = ? AND second_id = ?", idB, idA);
    }

    @Override
    public boolean exists(Long idA, Long idB) {
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mirrors WHERE (first_id = ? AND second_id = ?) OR (first_id = ? AND second_id = ?);",
                Integer.class,
                idA, idB, idB, idA
        )).orElse(0) > 0;
    }
}
