package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.preferences.UserPreferencesStore;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class TexListener extends ListenerAdapter {

    private static final long TIMEOUT_MINUTES = 10;
    @NotNull private static final Emoji CONFIRM_EMOJI = Emoji.fromUnicode("✅");
    @NotNull private static final Emoji REJECT_EMOJI  = Emoji.fromUnicode("🗑️");
    private static final String ERROR_BUTTON_PREFIX   = "tex-error-dismiss-";
    private static final Pattern LATEX_TRIGGER = Pattern.compile("(?<!\\\\)(\\$\\$[^$]+?\\$\\$|\\$[^$]+?\\$)", Pattern.DOTALL);

    // Shared across all guild instances — safe because Discord snowflakes are globally unique
    private static final Map<Long, TexEntry> IN_PROCESS          = new ConcurrentHashMap<>();
    private static final Map<Long, Long>     WEBHOOK_TO_ORIGINAL = new ConcurrentHashMap<>();
    private static final Set<Long>           IGNORED             = ConcurrentHashMap.newKeySet();
    // Error message tracking: original msg ↔ error reply msg
    private static final Map<Long, Long>     ORIGINAL_TO_ERROR   = new ConcurrentHashMap<>();
    private static final Map<Long, Long>     ERROR_TO_ORIGINAL   = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER      = new ScheduledThreadPoolExecutor(2);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    private final long guildId;

    TexListener(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        RenderedTex.evictChannel(event.getChannel().getIdLong());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;
        if (event.getMessage().isWebhookMessage()) return;
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        if (!LATEX_TRIGGER.matcher(message.getContentRaw()).find()) return;
        if (IGNORED.contains(message.getIdLong())) return;

        processMessage(message);
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        long msgId = event.getMessageIdLong();
        if (IGNORED.contains(msgId)) return;

        String newContent = event.getMessage().getContentRaw();

        // If already in-process (webhook exists), re-render and update it
        if (IN_PROCESS.containsKey(msgId)) {
            TexEntry entry = IN_PROCESS.get(msgId);
            boolean dark = UserPreferencesStore.isDarkTheme(entry.authorId, entry.guildId);
            LatexRenderer.renderToImage(newContent, dark)
                    .thenAccept(imageBytes -> {
                        // Success: clean up any error from a previous failed update
                        clearError(msgId, entry.channelId);
                        entry.imageBytes = imageBytes;
                        entry.renderedTex.update(entry.webhookMessageId, imageBytes);
                        if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);
                        entry.timeoutFuture = SCHEDULER.schedule(
                                () -> triggerConfirm(entry), TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    })
                    .exceptionally(t -> {
                        // Failure: replace any stale error, keep old webhook image
                        clearError(msgId, entry.channelId);
                        handleRenderError(t, event.getMessage());
                        return null;
                    });
            return;
        }

        // Clean up any existing compile-error reply for this message, then re-parse
        clearError(msgId, event.getChannel().getIdLong());

        if (!LATEX_TRIGGER.matcher(newContent).find()) return;
        processMessage(event.getMessage());
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        long deletedId = event.getMessageIdLong();

        // Handle deletion of the original (LaTeX) message
        TexEntry entry = IN_PROCESS.remove(deletedId);
        if (entry != null) {
            WEBHOOK_TO_ORIGINAL.remove(entry.webhookMessageId);
            if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);
            GuildMessageChannel ch = getChannel(entry.channelId);
            if (ch != null) ch.deleteMessageById(entry.webhookMessageId).queue(null, _ -> {});
        }

        // Clean up any error reply associated with this original message
        Long errorMsgId = ORIGINAL_TO_ERROR.remove(deletedId);
        if (errorMsgId != null) ERROR_TO_ORIGINAL.remove(errorMsgId);

        // Handle deletion of the error reply message itself (e.g. deleted by a moderator)
        Long origId = ERROR_TO_ORIGINAL.remove(deletedId);
        if (origId != null) ORIGINAL_TO_ERROR.remove(origId);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        User user = event.getUser();
        if (user == null || user.isBot()) return;

        Long origId = WEBHOOK_TO_ORIGINAL.get(event.getMessageIdLong());
        if (origId == null) return;

        TexEntry entry = IN_PROCESS.get(origId);
        if (entry == null) return;
        if (event.getUserIdLong() != entry.authorId) return;

        String emoji = normalizeEmoji(event.getEmoji().getName());
        if (emoji.equals(normalizeEmoji(CONFIRM_EMOJI.getName()))) {
            triggerConfirm(entry);
        } else if (emoji.equals(normalizeEmoji(REJECT_EMOJI.getName()))) {
            triggerReject(entry);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        String buttonId = event.getComponentId();
        if (!buttonId.startsWith(ERROR_BUTTON_PREFIX)) return;

        // Button ID: "tex-error-dismiss-{originalMsgId}-{authorId}"
        String[] parts = buttonId.substring(ERROR_BUTTON_PREFIX.length()).split("-");
        if (parts.length != 2) return;

        long originalMsgId;
        long authorId;
        try {
            originalMsgId = Long.parseLong(parts[0]);
            authorId      = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        if (event.getUser().getIdLong() != authorId) {
            event.reply(Strings.getString("command.tex.error_not_author")).setEphemeral(true).queue();
            return;
        }

        Long errorMsgId = ORIGINAL_TO_ERROR.remove(originalMsgId);
        if (errorMsgId != null) ERROR_TO_ORIGINAL.remove(errorMsgId);
        IGNORED.add(originalMsgId);

        event.deferEdit().queue(hook -> hook.deleteOriginal().queue(null, _ -> {}));
    }

    // -------------------------------------------------------------------------

    private void processMessage(Message message) {
        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel();
        Member author   = message.getMember();
        long authorId   = message.getAuthor().getIdLong();
        long originalId = message.getIdLong();
        long channelId  = channel.getIdLong();
        long guild      = guildId;

        Message referenced    = message.getReferencedMessage();
        String replyJumpUrl   = referenced != null ? referenced.getJumpUrl() : null;
        String replyLabel     = referenced != null ? authorDisplayName(referenced) : null;
        String content        = message.getContentRaw();

        boolean dark = UserPreferencesStore.isDarkTheme(authorId, guild);
        RenderedTex rt = new RenderedTex(channel, author);

        LatexRenderer.renderToImage(content, dark)
                .thenAccept(imageBytes ->
                        rt.send(imageBytes, replyJumpUrl, replyLabel, webhookMsg -> {
                            TexEntry entry = new TexEntry(originalId, authorId, channelId, guild,
                                    replyJumpUrl, replyLabel, imageBytes,
                                    webhookMsg.getIdLong(), rt);

                            IN_PROCESS.put(originalId, entry);
                            WEBHOOK_TO_ORIGINAL.put(webhookMsg.getIdLong(), originalId);

                            webhookMsg.addReaction(CONFIRM_EMOJI).queue(
                                    _ -> webhookMsg.addReaction(REJECT_EMOJI).queue());

                            entry.timeoutFuture = SCHEDULER.schedule(
                                    () -> triggerConfirm(entry), TIMEOUT_MINUTES, TimeUnit.MINUTES);
                        })
                )
                .exceptionally(t -> {
                    handleRenderError(t, message);
                    return null;
                });
    }

    private static void triggerConfirm(TexEntry entry) {
        if (!IN_PROCESS.remove(entry.originalMessageId, entry)) return;
        WEBHOOK_TO_ORIGINAL.remove(entry.webhookMessageId);
        if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);

        GuildMessageChannel channel = getChannel(entry.channelId);
        if (channel == null) return;

        if (entry.replyJumpUrl != null) {
            channel.deleteMessageById(entry.webhookMessageId).queue(
                    _ -> entry.renderedTex.send(entry.imageBytes,
                            entry.replyJumpUrl, entry.replyButtonLabel, _ -> {}),
                    _ -> {}
            );
        } else {
            channel.retrieveMessageById(entry.webhookMessageId).queue(
                    webhookMsg -> webhookMsg.clearReactions().queue(), _ -> {});
        }

        channel.deleteMessageById(entry.originalMessageId).queue(null, _ -> {});
    }

    private static void triggerReject(TexEntry entry) {
        if (!IN_PROCESS.remove(entry.originalMessageId, entry)) return;
        WEBHOOK_TO_ORIGINAL.remove(entry.webhookMessageId);
        if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);

        GuildMessageChannel channel = getChannel(entry.channelId);
        if (channel != null)
            channel.deleteMessageById(entry.webhookMessageId).queue(null, _ -> {});

        IGNORED.add(entry.originalMessageId);
    }

    private static void handleRenderError(Throwable t, Message originalMessage) {
        Throwable cause = t.getCause() != null ? t.getCause() : t;
        if (!(cause instanceof LatexRenderer.LatexCompilationException)) {
            Main.LOGGER.warn("[TexModule] Unexpected render error: {}", t.getMessage());
            return;
        }

        String log = cause.getMessage();
        String[] lines = log.split("\n");
        int start = Math.max(0, lines.length - 10);
        StringBuilder excerpt = new StringBuilder();
        for (int i = start; i < lines.length; i++) excerpt.append(lines[i]).append('\n');
        String trimmed = excerpt.toString().strip();
        if (trimmed.length() > 1800) trimmed = trimmed.substring(trimmed.length() - 1800);

        long originalMsgId = originalMessage.getIdLong();
        long authorId      = originalMessage.getAuthor().getIdLong();

        originalMessage.reply("❌ LaTeX compilation error:\n```\n" + trimmed + "\n```")
                .addComponents(ActionRow.of(
                        Button.danger(ERROR_BUTTON_PREFIX + originalMsgId + "-" + authorId, "🗑️ Dismiss")
                ))
                .queue(errorMsg -> {
                    ORIGINAL_TO_ERROR.put(originalMsgId, errorMsg.getIdLong());
                    ERROR_TO_ORIGINAL.put(errorMsg.getIdLong(), originalMsgId);
                });
    }

    private static void clearError(long originalMsgId, long channelId) {
        Long errorMsgId = ORIGINAL_TO_ERROR.remove(originalMsgId);
        if (errorMsgId == null) return;
        ERROR_TO_ORIGINAL.remove(errorMsgId);
        GuildMessageChannel ch = getChannel(channelId);
        if (ch != null) ch.deleteMessageById(errorMsgId).queue(null, _ -> {});
    }

    @Nullable
    private static GuildMessageChannel getChannel(long channelId) {
        return Main.getJDA().getChannelById(GuildMessageChannel.class, channelId);
    }

    private static String authorDisplayName(Message msg) {
        Member member = msg.getMember();
        if (member != null && member.getNickname() != null) return member.getNickname();
        return msg.getAuthor().getEffectiveName();
    }

    private static String normalizeEmoji(String emoji) {
        return emoji.replace("️", ""); // strip variation selector U+FE0F
    }

    static class TexEntry {
        final long originalMessageId;
        final long authorId;
        final long channelId;
        final long guildId;
        @Nullable final String replyJumpUrl;
        @Nullable final String replyButtonLabel;
        final RenderedTex renderedTex;
        volatile byte[] imageBytes;
        volatile long webhookMessageId;
        volatile ScheduledFuture<?> timeoutFuture;

        TexEntry(long originalMessageId, long authorId, long channelId, long guildId,
                 @Nullable String replyJumpUrl, @Nullable String replyButtonLabel,
                 byte[] imageBytes, long webhookMessageId, RenderedTex renderedTex) {
            this.originalMessageId = originalMessageId;
            this.authorId          = authorId;
            this.channelId         = channelId;
            this.guildId           = guildId;
            this.replyJumpUrl      = replyJumpUrl;
            this.replyButtonLabel  = replyButtonLabel;
            this.imageBytes        = imageBytes;
            this.webhookMessageId  = webhookMessageId;
            this.renderedTex       = renderedTex;
        }
    }
}
