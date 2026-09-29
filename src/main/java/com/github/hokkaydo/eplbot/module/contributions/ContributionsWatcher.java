package com.github.hokkaydo.eplbot.module.contributions;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.contributions.DriveLister.DriveFile;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodically lists the Drive contributions folder and announces, one message per file,
 * the files uploaded since the last check in the Drive admin channel.
 * <br>
 * Only the upload time of the most recent file is kept (guild state {@code CONTRIBUTIONS_LAST_UPLOAD}),
 * so a file moved out of the folder (e.g. when a contribution is imported) triggers nothing. The first listing
 * only records that time and sends a single summary message, so enabling the module does not flood the channel.
 * */
class ContributionsWatcher {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String LAST_UPLOAD_KEY = "CONTRIBUTIONS_LAST_UPLOAD";

    private final Long guildId;
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private String lastError;

    ContributionsWatcher(Long guildId) {
        this.guildId = guildId;
    }

    void launch() {
        if (task != null && !task.isCancelled()) stop();
        if (service.isShutdown()) service = Executors.newSingleThreadScheduledExecutor();
        // A period <= 0 makes scheduleAtFixedRate throw, which would break the module enabling at startup
        long period = Math.max(1L, Config.<Long>getGuildVariable(guildId, "CONTRIBUTIONS_UPDATE_PERIOD"));
        task = service.scheduleAtFixedRate(this::run, 0, period, TimeUnit.MINUTES);
    }

    void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        service.shutdown();
    }

    private void run() {
        // An exception escaping a scheduleAtFixedRate task silently cancels all its future runs
        try {
            check();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | IOException e) {
            Main.LOGGER.error("[Contributions] Error while checking contributions", e);
            reportError(Strings.getString("contributions.listing_error").formatted(DriveLister.getRemote(), truncate(e.getMessage())));
        }
    }

    private void check() throws IOException, InterruptedException {
        List<DriveFile> files = DriveLister.list();
        Guild guild = Main.getJDA().getGuildById(guildId);
        if (guild == null) return;
        Optional<TextChannel> channel = getChannel(guild);
        // In both cases, keep the last upload time: the new files will be announced once the channel is usable
        if (channel.isEmpty()) {
            reportError(Strings.getString("drive_admin_channel_not_setup"));
            return;
        }
        if (!channel.get().canTalk()) {
            reportError(Strings.getString("contributions.channel_no_permission").formatted(channel.get().getAsMention()));
            return;
        }

        Instant lastUpload = Config.getGuildState(guildId, LAST_UPLOAD_KEY);
        boolean firstListing = lastUpload.equals(Instant.MIN);
        List<DriveFile> newFiles = files.stream().filter(f -> f.uploadTime().isAfter(lastUpload)).toList();

        if (firstListing) {
            send(channel.get(), Strings.getString("contributions.enabled").formatted(files.size()), null);
        } else if (!newFiles.isEmpty()) {
            // Only the first message of a batch mentions the role, so that a big contribution pings once
            Role role = getRole(guild);
            for (int i = 0; i < newFiles.size(); i++)
                send(channel.get(), Strings.getString("contributions.new").formatted(newFiles.get(i).displayPath(), newFiles.get(i).displaySize()), i == 0 ? role : null);
        }

        // An empty folder at first listing still counts as listed, so that its first file gets announced
        Instant newest = newFiles.stream().map(DriveFile::uploadTime).max(Instant::compareTo).orElse(firstListing ? Instant.EPOCH : lastUpload);
        if (!newest.equals(lastUpload))
            Config.updateValue(guildId, LAST_UPLOAD_KEY, newest);
        lastError = null;
    }

    private void send(TextChannel channel, String content, Role mention) {
        // File names come from contributors: never let them ping anyone, only whitelist the configured role
        MessageCreateAction action = channel.sendMessage(mention == null ? content : "%s %s".formatted(mention.getAsMention(), content))
                                             .setAllowedMentions(Collections.emptySet());
        if (mention != null) action = action.mentionRoles(mention.getId());
        action.queue(null, e -> Main.LOGGER.error("[Contributions] Could not send message in {}", channel.getName(), e));
    }

    private Optional<TextChannel> getChannel(Guild guild) {
        String channelId = Config.getGuildVariable(guildId, "DRIVE_ADMIN_CHANNEL_ID");
        if (channelId.isBlank()) return Optional.empty();
        try {
            return Optional.ofNullable(guild.getTextChannelById(channelId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Role getRole(Guild guild) {
        String roleId = Config.getGuildVariable(guildId, "CONTRIBUTIONS_ROLE_ID");
        if (roleId.isBlank()) return null;
        Role role;
        try {
            role = guild.getRoleById(roleId);
        } catch (NumberFormatException e) {
            role = null;
        }
        if (role == null) Main.LOGGER.warn("[Contributions] Invalid CONTRIBUTIONS_ROLE_ID '{}', sending without mention", roleId);
        return role;
    }

    /**
     * Sends an error to the admin channel, only once per distinct error to avoid spamming it every run
     * */
    private void reportError(String message) {
        if (message.equals(lastError)) return;
        lastError = message;
        Main.LOGGER.warn("[Contributions] {}", message);
        MessageUtil.sendAdminMessage(message, guildId);
    }

    private static String truncate(String message) {
        if (message == null) return "";
        // Keep the end: rclone's last lines hold the actual reason of the failure
        return message.length() > MAX_ERROR_LENGTH ? "..." + message.substring(message.length() - MAX_ERROR_LENGTH) : message;
    }

}
