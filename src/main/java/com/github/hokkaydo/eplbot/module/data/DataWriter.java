package com.github.hokkaydo.eplbot.module.data;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;

public class DataWriter {

    private final JdbcTemplate jdbcTemplate;
    private final long guildId;

    public DataWriter(DataSource dataSource, long guildId) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.guildId = guildId;
    }

    /**
     * Logs a message received event
     * @param userId the ID of the user who sent the message
     * @param channelId the ID of the channel where the message was sent
     */
    public void logMessageReceived(long userId, long channelId) {
        logEvent("MESSAGE_RECEIVED", userId, channelId);
    }

    /**
     * Logs a message reaction add event
     * @param userId the ID of the user who added the reaction
     * @param channelId the ID of the channel where the reaction was added
     * @param emoji the emoji that was added
     */
    public void logReactionAdded(long userId, long channelId, String emoji) {
        long timestamp = Instant.now().getEpochSecond();
        
        jdbcTemplate.update(
            "INSERT INTO events (timestamp, event_type, user_id, channel_id, reaction_emoji, guild_id) VALUES (?, ?, ?, ?, ?, ?)",
            timestamp, "REACTION_ADDED", userId, channelId, emoji, guildId
        );
    }

    /**
     * Logs a member join event
     * @param userId the ID of the user who joined
     */
    public void logMemberJoin(long userId) {
        logEvent("MEMBER_JOIN", userId, null);
    }

    /**
     * Logs a member remove event
     * @param userId the ID of the user who left/was removed
     */
    public void logMemberRemove(long userId) {
        logEvent("MEMBER_REMOVE", userId, null);
    }

    /**
     * Logs an event to the database
     * @param eventType the type of event
     * @param userId the ID of the user involved
     * @param channelId the ID of the channel (can be null for member events)
     */
    private void logEvent(String eventType, long userId, Long channelId) {
        long timestamp = Instant.now().getEpochSecond();
        
        if (channelId != null) {
            jdbcTemplate.update(
                "INSERT INTO events (timestamp, event_type, user_id, channel_id, guild_id) VALUES (?, ?, ?, ?, ?)",
                timestamp, eventType, userId, channelId, guildId
            );
        } else {
            jdbcTemplate.update(
                "INSERT INTO events (timestamp, event_type, user_id, channel_id, guild_id) VALUES (?, ?, ?, NULL, ?)",
                timestamp, eventType, userId, guildId
            );
        }
    }

}
