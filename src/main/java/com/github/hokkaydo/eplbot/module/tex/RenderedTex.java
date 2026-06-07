package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.module.mirror.WebhookWithMessage;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.entities.WebhookImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RenderedTex {

    private static final Map<Long, WebhookWithMessage> CHANNEL_WEBHOOK = new HashMap<>();
    private static final String DEFAULT_WEBHOOK_NAME = "TEX_WEBHOOK";

    private final GuildMessageChannel channel;
    private final boolean threadChannel;
    private final String authorName;
    private final String authorAvatarUrl;

    public RenderedTex(GuildMessageChannel channel, Member author) {
        this.channel = channel;
        this.threadChannel = channel instanceof ThreadChannel;
        String nick = author != null ? author.getNickname() : null;
        this.authorName = (nick != null ? nick
                : author != null ? author.getUser().getEffectiveName()
                : "Unknown").replace("everyone", "everyonⲉ");
        this.authorAvatarUrl = Optional.ofNullable(
                        author != null ? author.getUser().getAvatar() : null)
                .orElse(Main.getJDA().getSelfUser().getDefaultAvatar())
                .getUrl();
    }

    /**
     * Send the rendered image via webhook. If replyJumpUrl is provided, adds a button
     * linking to the referenced message (used instead of Discord native reply since
     * WebhookMessageCreateAction does not expose setMessageReference).
     */
    void send(@NotNull byte[] imageBytes, @Nullable String replyJumpUrl, @Nullable String replyButtonLabel,
              Consumer<Message> onSent) {
        getWebhook().thenAccept(webhook -> {
            var action = webhook.sendRequest()
                    .setUsername(authorName)
                    .setAvatarUrl(authorAvatarUrl)
                    .addFiles(FileUpload.fromData(imageBytes, "rendered.png"));

            if (replyJumpUrl != null && replyButtonLabel != null)
                action.addComponents(ActionRow.of(Button.link(replyJumpUrl, "↪ " + replyButtonLabel)));

            action.queue(onSent);
        });
    }

    void update(long webhookMessageId, @NotNull byte[] imageBytes) {
        getWebhook().thenAccept(webhook ->
                webhook.editRequest(String.valueOf(webhookMessageId))
                        .setAttachments(FileUpload.fromData(imageBytes, "rendered.png"))
                        .queue()
        );
    }

    private CompletableFuture<WebhookWithMessage> getWebhook() {
        if (CHANNEL_WEBHOOK.containsKey(channel.getIdLong()))
            return CompletableFuture.completedFuture(CHANNEL_WEBHOOK.get(channel.getIdLong()));

        IWebhookContainer container = getWebhookContainer();
        long selfId = Main.getJDA().getSelfUser().getIdLong();

        return container.retrieveWebhooks().submit().thenCompose(webhooks -> {
            Optional<Webhook> existing = webhooks.stream()
                    .filter(w -> {
                        var owner = w.getOwner();
                        return w.getToken() != null && owner != null && owner.getIdLong() == selfId;
                    })
                    .findFirst();

            if (existing.isPresent()) {
                WebhookWithMessage wh = new WebhookWithMessage(
                        (WebhookImpl) existing.get(), threadChannel, channel.getIdLong());
                CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
                return CompletableFuture.completedFuture(wh);
            }

            return container.createWebhook(DEFAULT_WEBHOOK_NAME).submit().thenApply(newWebhook -> {
                WebhookWithMessage wh = new WebhookWithMessage(
                        (WebhookImpl) newWebhook, threadChannel, channel.getIdLong());
                CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
                return wh;
            });
        });
    }

    private IWebhookContainer getWebhookContainer() {
        if (channel instanceof ThreadChannel tc) {
            if (!(tc.getParentChannel() instanceof IWebhookContainer parent))
                throw new IllegalStateException("Thread parent is not webhook-capable");
            return parent;
        }
        if (!(channel instanceof IWebhookContainer wc))
            throw new IllegalStateException("Channel is not webhook-capable");
        return wc;
    }

    static void evictChannel(long channelId) {
        CHANNEL_WEBHOOK.remove(channelId);
    }
}
