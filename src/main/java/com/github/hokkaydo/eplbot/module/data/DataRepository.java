package com.github.hokkaydo.eplbot.module.data;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DataRepository {

    private final JdbcTemplate jdbcTemplate;

    public DataRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Get the most active channels based on message count
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of channels to return
     * @return a {@link Set} of {@link ActiveChannel} sorted by message count descending
     */
    public Set<ActiveChannel> getMostActiveChannels(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT channel_id, COUNT(*) as count FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ? AND channel_id IS NOT NULL " +
            "GROUP BY channel_id ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Set<ActiveChannel> channelCounts = new TreeSet<>((a, b) -> Long.compare(b.messageCount(), a.messageCount()));
        for (Map<String, Object> row : results) {
            long count = ((Number) row.get("count")).longValue();
            long channelId = ((Number) row.get("channel_id")).longValue();
            channelCounts.add(new ActiveChannel(channelId, count));
        }
        return channelCounts;
    }

    /**
     * Get the most active hours based on message count
     * @param durationDays the duration in days to look back
     * @return a {@link Set} of {@link ActiveHour} sorted by message count descending
     */
    public Set<ActiveHour> getMostActiveHours(int durationDays) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT timestamp FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ?",
            cutoffTimestamp
        );

        // hour to count map
        Map<Integer, Long> hourCountMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCountMap.put(i, 0L);
        }

        for (Map<String, Object> row : results) {
            long timestamp = ((Number) row.get("timestamp")).longValue();
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
            );
            int hour = dateTime.getHour();
            hourCountMap.put(hour, hourCountMap.get(hour) + 1);
        }
        for (int hour : hourCountMap.keySet()) {
            hourCountMap.put(hour, hourCountMap.get(hour) / durationDays);
        }
        return hourCountMap.entrySet().stream()
            .map(e -> new ActiveHour(e.getKey(), e.getValue()))
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingLong(ActiveHour::hour))));
    }

    /**
     * Get the most active users based on message count
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of users to return
     * @return a {@link Set} of {@link ActiveUser} sorted by message count descending
     */
    public Set<ActiveUser> getMostActiveUsers(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT user_id, COUNT(*) as count FROM events " +
            "WHERE event_type = 'MESSAGE_RECEIVED' AND timestamp > ? AND user_id IS NOT NULL " +
            "GROUP BY user_id ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Set<ActiveUser> userCounts = new TreeSet<>((a, b) -> Long.compare(b.messageCount(), a.messageCount()));
        for (Map<String, Object> row : results) {
            long userId = ((Number) row.get("user_id")).longValue();
            long count = ((Number) row.get("count")).longValue();
            userCounts.add(new ActiveUser(userId, count));
        }
        return userCounts;
    }

    /**
     * Get the top most used reactions
     * @param durationDays the duration in days to look back
     * @param limit the maximum number of reactions to return
     * @return a {@link Set} of {@link ReactionCount} sorted by count descending
     */
    public Set<ReactionCount> getTopReactions(int durationDays, int limit) {
        long cutoffTimestamp = Instant.now().getEpochSecond() - (durationDays * 86400L);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT reaction_emoji, COUNT(*) as count FROM events " +
            "WHERE event_type = 'REACTION_ADDED' AND timestamp > ? AND reaction_emoji IS NOT NULL " +
            "GROUP BY reaction_emoji ORDER BY count DESC LIMIT ?",
            cutoffTimestamp, limit
        );

        Set<ReactionCount> reactionCounts = new TreeSet<>((a, b) -> Long.compare(b.count(), a.count()));
        for (Map<String, Object> row : results) {
            String emoji = (String) row.get("reaction_emoji");
            long count = ((Number) row.get("count")).longValue();
            reactionCounts.add(new ReactionCount(emoji, count));
        }
        return reactionCounts;
    }


    public record ActiveChannel(long channelId, long messageCount) {}


    public record ActiveHour(int hour, long messageCount) {}

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


    public record ActiveUser(long userId, long messageCount) {}


    public record ReactionCount(String reactionEmoji, long count) {}

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
