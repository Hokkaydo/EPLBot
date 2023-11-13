package com.github.hokkaydo.eplbot.module.mirror.repository;

import com.github.hokkaydo.eplbot.module.mirror.model.MirrorLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;

public class MirrorLinkRepositorySQLite implements MirrorLinkRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<MirrorLink> mapper = (ResultSet rs, int numRow) -> new MirrorLink(rs.getLong("channelIdA"), rs.getLong("channelIdB"));

    public MirrorLinkRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(MirrorLink... mirrorLinks) {
        for (MirrorLink mirrorLink : mirrorLinks) {
            jdbcTemplate.update("""
                INSERT INTO mirrors (
                    channelIdA,
                    channelIdB
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
                "SELECT * FROM mirrors WHERE channelIdA = ? OR channelIdB = ?",
                mapper,
                channelId, channelId
        ).toList();
    }


    @Override
    public void deleteByIds(Long idA, Long idB) {
        jdbcTemplate.update("DELETE FROM mirrors WHERE channelIdA = ? AND channelIdB = ?", idA, idB);
        jdbcTemplate.update("DELETE FROM mirrors WHERE channelIdA = ? AND channelIdB = ?", idB, idA);
    }

    @Override
    public boolean exists(Long idA, Long idB) {
        return Boolean.TRUE.equals(jdbcTemplate.query(
                "SELECT COUNT(1) FROM mirrors WHERE (channelIdA = ? AND channelIdB = ?) OR (channelIdA = ? AND channelIdB = ?)",
                ResultSet::first,
                idA, idB, idB, idA
        ));
    }

    @Override
    public List<MirrorLink> all() {
        return jdbcTemplate.query(
                "SELECT * FROM mirrors",
                mapper
        );
    }

}
