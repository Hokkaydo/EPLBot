package com.github.hokkaydo.eplbot.module.contributions.repository;

import com.github.hokkaydo.eplbot.module.contributions.model.ContributionFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContributionFileRepositorySQLite implements ContributionFileRepository {

    private static final RowMapper<ContributionFile> MAPPER = (rs, ignored) -> new ContributionFile(
            rs.getLong("guild_id"),
            rs.getString("file_id")
    );

    private final JdbcTemplate jdbcTemplate;

    public ContributionFileRepositorySQLite(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void create(ContributionFile... models) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO contribution_files (guild_id, file_id) VALUES (?, ?)",
                Arrays.stream(models).map(m -> new Object[]{m.guildId(), m.fileId()}).toList()
        );
    }

    @Override
    public List<ContributionFile> readAll() {
        return jdbcTemplate.query("SELECT * FROM contribution_files", MAPPER);
    }

    @Override
    public Set<String> getFileIds(long guildId) {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT file_id FROM contribution_files WHERE guild_id=?", String.class, guildId));
    }

    @Override
    public boolean isSeeded(long guildId, String remote) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contribution_remotes WHERE guild_id=? AND remote=?", Integer.class, guildId, remote);
        return count != null && count > 0;
    }

    @Override
    public void markSeeded(long guildId, String remote) {
        jdbcTemplate.update("INSERT INTO contribution_remotes (guild_id, remote) VALUES (?, ?)", guildId, remote);
    }

}
