package com.github.hokkaydo.eplbot.module.data;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRepository {

    private final JdbcTemplate jdbcTemplate;

    public DataRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Get the most active channels based on message count
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of channels to return
     * @return a map of channel IDs to message counts
     */
    public Map<Long, Long> getMostActiveChannels(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT channel_id, COUNT(*) as count FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ? AND channel_id IS NOT NULL " +
            "GROUP BY channel_id ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Map<Long, Long> channelCounts = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long channelId = ((Number) row.get("channel_id")).longValue();
            Long count = ((Number) row.get("count")).longValue();
            channelCounts.put(channelId, count);
        }
        return channelCounts;
    }

    /**
     * Get member join/leave events over time
     * @param durationDays the duration in days to look back
     * @return a map with "joins" and "leaves" lists containing timestamps
     */
    public Map<String, List<Long>> getMemberEventsOverTime(int durationDays) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);
        
        List<Long> joins = jdbcTemplate.queryForList(
            "SELECT timestamp FROM events WHERE event_type = 'MEMBER_JOIN' AND timestamp > ? ORDER BY timestamp",
            Long.class,
            cutoffTimestamp
        );
        
        List<Long> leaves = jdbcTemplate.queryForList(
            "SELECT timestamp FROM events WHERE event_type = 'MEMBER_REMOVE' AND timestamp > ? ORDER BY timestamp",
            Long.class,
            cutoffTimestamp
        );
        
        return Map.of("joins", joins, "leaves", leaves);
    }

    /**
     * Get the most active users based on message count
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of users to return
     * @return a map of user IDs to message counts
     */
    public Map<Long, Long> getMostActiveUsers(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT user_id, COUNT(*) as count FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ? AND user_id IS NOT NULL " +
            "GROUP BY user_id ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Map<Long, Long> userCounts = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long userId = ((Number) row.get("user_id")).longValue();
            Long count = ((Number) row.get("count")).longValue();
            userCounts.put(userId, count);
        }
        return userCounts;
    }

    /**
     * Get the top most used reactions
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of reactions to return
     * @return a map of reaction emojis to counts
     */
    public Map<String, Long> getTopReactions(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT reaction_emoji, COUNT(*) as count FROM events " +
            "WHERE event_type = 'REACTION_ADDED' AND timestamp > ? AND reaction_emoji IS NOT NULL " +
            "GROUP BY reaction_emoji ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Map<String, Long> reactionCounts = new HashMap<>();
        for (Map<String, Object> row : results) {
            String emoji = (String) row.get("reaction_emoji");
            Long count = ((Number) row.get("count")).longValue();
            reactionCounts.put(emoji, count);
        }
        return reactionCounts;
    }

    /**
     * Get message counts for specific channels
     * @param channelIds the list of channel IDs
     * @param durationDays the duration in days to look back
     * @return a map of channel IDs to message counts
     */
    public Map<Long, Long> getMessageCountsByChannels(List<Long> channelIds, int durationDays) {
        if (channelIds.isEmpty()) return new HashMap<>();
        
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);
        
        String inClause = String.join(",", channelIds.stream().map(String::valueOf).toList());
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT channel_id, COUNT(*) as count FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ? AND channel_id IN (" + inClause + ") " +
            "GROUP BY channel_id",
            cutoffTimestamp
        );

        Map<Long, Long> channelCounts = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long channelId = ((Number) row.get("channel_id")).longValue();
            Long count = ((Number) row.get("count")).longValue();
            channelCounts.put(channelId, count);
        }
        return channelCounts;
    }

    /**
     * Delete events older than the specified number of days
     * @param retentionDays the number of days to retain data
     * @return the number of rows deleted
     */
    public int cleanupOldEvents(int retentionDays) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (retentionDays * 86400L);
        
        return jdbcTemplate.update(
            "DELETE FROM events WHERE timestamp < ?",
            cutoffTimestamp
        );
    }

}
