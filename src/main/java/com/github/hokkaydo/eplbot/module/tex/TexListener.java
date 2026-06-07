package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
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
    private static final Pattern LATEX_TRIGGER =
            Pattern.compile("(?<!\\\\)(\\$\\$[^$]+?\\$\\$|\\$[^$\\\\]+?\\$)", Pattern.DOTALL);

    // Shared across all guild instances — safe because Discord snowflakes are globally unique
    private static final Map<Long, TexEntry> IN_PROCESS          = new ConcurrentHashMap<>();
    private static final Map<Long, Long>     WEBHOOK_TO_ORIGINAL = new ConcurrentHashMap<>();
    private static final Set<Long>           IGNORED             = ConcurrentHashMap.newKeySet();
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
        String content = message.getContentRaw();
        if (!LATEX_TRIGGER.matcher(content).find()) return;

        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel();
        Member author = message.getMember();

        Message referenced = message.getReferencedMessage();
        String replyJumpUrl = referenced != null ? referenced.getJumpUrl() : null;
        String replyButtonLabel = referenced != null ? authorDisplayName(referenced) : null;

        RenderedTex rt = new RenderedTex(channel, author);
        long originalId = message.getIdLong();
        long authorId   = message.getAuthor().getIdLong();
        long channelId  = channel.getIdLong();

        LatexRenderer.renderToImage(content)
                .thenAccept(imageBytes ->
                        rt.send(imageBytes, replyJumpUrl, replyButtonLabel, webhookMsg -> {
                            TexEntry entry = new TexEntry(originalId, authorId, channelId,
                                    replyJumpUrl, replyButtonLabel, imageBytes,
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

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        long msgId = event.getMessageIdLong();
        if (!IN_PROCESS.containsKey(msgId) || IGNORED.contains(msgId)) return;

        TexEntry entry = IN_PROCESS.get(msgId);
        String newContent = event.getMessage().getContentRaw();

        LatexRenderer.renderToImage(newContent)
                .thenAccept(imageBytes -> {
                    entry.imageBytes = imageBytes;
                    entry.renderedTex.update(entry.webhookMessageId, imageBytes);

                    if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);
                    entry.timeoutFuture = SCHEDULER.schedule(
                            () -> triggerConfirm(entry), TIMEOUT_MINUTES, TimeUnit.MINUTES);
                })
                .exceptionally(t -> {
                    handleRenderError(t, event.getMessage());
                    return null;
                });
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != guildId) return;

        TexEntry entry = IN_PROCESS.remove(event.getMessageIdLong());
        if (entry == null) return;

        WEBHOOK_TO_ORIGINAL.remove(entry.webhookMessageId);
        if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);

        GuildMessageChannel channel = getChannel(entry.channelId);
        if (channel != null)
            channel.deleteMessageById(entry.webhookMessageId).queue(null, _ -> {});
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

    private static void triggerConfirm(TexEntry entry) {
        if (!IN_PROCESS.remove(entry.originalMessageId, entry)) return;
        WEBHOOK_TO_ORIGINAL.remove(entry.webhookMessageId);
        if (entry.timeoutFuture != null) entry.timeoutFuture.cancel(false);

        GuildMessageChannel channel = getChannel(entry.channelId);
        if (channel == null) return;

        if (entry.replyJumpUrl != null) {
            // Original was a reply — delete preview and resend as reply to grandparent
            channel.deleteMessageById(entry.webhookMessageId).queue(
                    _ -> entry.renderedTex.send(entry.imageBytes,
                            entry.replyJumpUrl, entry.replyButtonLabel, _ -> {}),
                    _ -> {}
            );
        } else {
            // Keep the preview in place, just strip reactions
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
        return emoji.replace("️", ""); // strip variation selector
    }

    private static void handleRenderError(Throwable t, Message originalMessage) {
        Throwable cause = t.getCause() != null ? t.getCause() : t;
        if (cause instanceof LatexRenderer.LatexCompilationException) {
            String log = cause.getMessage();
            // Extract a compact error: last ~10 lines from pdflatex log
            String[] lines = log.split("\n");
            int start = Math.max(0, lines.length - 10);
            StringBuilder excerpt = new StringBuilder();
            for (int i = start; i < lines.length; i++) excerpt.append(lines[i]).append('\n');
            String trimmed = excerpt.toString().strip();
            if (trimmed.length() > 1800) trimmed = trimmed.substring(trimmed.length() - 1800);
            originalMessage.reply("❌ LaTeX compilation error:\n```\n" + trimmed + "\n```").queue();
        } else {
            Main.LOGGER.warn("[TexModule] Unexpected render error: {}", t.getMessage());
        }
    }

    static class TexEntry {
        final long originalMessageId;
        final long authorId;
        final long channelId;
        @Nullable final String replyJumpUrl;
        @Nullable final String replyButtonLabel;
        final RenderedTex renderedTex;
        volatile byte[] imageBytes;
        volatile long webhookMessageId;
        volatile ScheduledFuture<?> timeoutFuture;

        TexEntry(long originalMessageId, long authorId, long channelId,
                 @Nullable String replyJumpUrl, @Nullable String replyButtonLabel,
                 byte[] imageBytes, long webhookMessageId, RenderedTex renderedTex) {
            this.originalMessageId = originalMessageId;
            this.authorId          = authorId;
            this.channelId         = channelId;
            this.replyJumpUrl      = replyJumpUrl;
            this.replyButtonLabel  = replyButtonLabel;
            this.imageBytes        = imageBytes;
            this.webhookMessageId  = webhookMessageId;
            this.renderedTex       = renderedTex;
        }
    }
}
