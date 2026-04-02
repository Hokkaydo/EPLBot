package com.github.hokkaydo.eplbot.module.translation;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.module.mirror.WebhookWithMessage;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.entities.WebhookImpl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a message that is being traduced using webhooks
 * Fully based on {@link com.github.hokkaydo.eplbot.module.mirror.MirroredMessage}
 */
public class TranslatedMessage {

    /** Map of channel IDs to their corresponding mirroring webhooks */
    private static final Map<Long, WebhookWithMessage> CHANNEL_WEBHOOK = new HashMap<>();
    /** Default name for the mirroring webhook */
    private static final String DEFAULT_WEBHOOK_NAME = "MIRROR_WEBHOOK";
    /** The original message to be mirrored */
    private final Message originalMessage;
    /** Channel where the mirroring is happening */
    private final GuildMessageChannel channel;
    /** Flag indicating if the mirroring is happening in a thread channel */
    private final boolean threadMirror;

    /** Create a new {@link TranslatedMessage} instance
     * @param initialMessage the original message to mirror
     * */
    public TranslatedMessage(Message initialMessage) {
        this.channel = (GuildMessageChannel) initialMessage.getChannel();
        this.threadMirror = channel instanceof ThreadChannel;
        this.originalMessage = initialMessage;
    }

    /**
     * Create the request to send a mirror message
     * @param replyTo the message {@link TranslatedMessage#originalMessage} responded to if it is an answer, can be null
     * */
    void createAndSendMessage(Message replyTo, String content, Consumer<Message> onSuccess) {
        originalMessage.getGuild()
                .findMembers(m ->
                                     m.getIdLong() == originalMessage.getAuthor().getIdLong()
                                             || (replyTo != null && m.getIdLong() == replyTo.getAuthor().getIdLong())
                )
                .onSuccess(members -> {

                    // Load icon of the author to give the webhook the same icon
                    String iconUrl = Optional.ofNullable(originalMessage.getAuthor().getAvatar())
                                             .orElse(Main.getJDA().getSelfUser().getDefaultAvatar())
                                             .getUrl();

                    // Determine denied mentions
                    List<Message.MentionType> deniedMentions = new ArrayList<>(List.of(Message.MentionType.USER));
                    Optional<Member> originalMember = members.stream().filter(m -> m.getIdLong() == originalMessage.getAuthor().getIdLong()).findFirst();
                    if  (originalMember.isEmpty()) {
                        Main.LOGGER.warn("[TranslationModule] Original message author not found as member entity in its own guild");
                        return;
                    }
                    if (!originalMember.get().hasPermission(Permission.MESSAGE_MENTION_EVERYONE)) {
                        deniedMentions.addAll(List.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE));
                    }

                    List<Long> membersId = members.stream().map(Member::getIdLong).toList();

                    final String name;
                    if (originalMember.get().getNickname() != null) {
                        name = originalMember.get().getNickname().replace("everyone", "everyonⲉ");
                    } else {
                        name = originalMember.get().getUser().getEffectiveName().replace("everyone", "everyonⲉ");
                    }

                    getWebhook().thenCompose(webhook -> {
                        WebhookMessageCreateAction<Message> createAction =
                                webhook.sendRequest()
                                        .setContent(content)
                                        .setAvatarUrl(iconUrl)
                                        .setUsername(name)
                                        .setAllowedMentions(EnumSet.complementOf(EnumSet.copyOf(deniedMentions)))
                                        // Filter mentions to only include users that are present in the mirror guild
                                        .mentionUsers(originalMessage.getMentions()
                                                              .getMentions(Message.MentionType.USER)
                                                              .stream()
                                                              .map(m -> (UserSnowflake) m)
                                                              .filter(u -> !membersId.contains(u.getIdLong()))
                                                              .map(UserSnowflake::getId).toList()
                                        );

                        // If the original message is a reply, add a button linking to the original replied message
                        if (replyTo != null) {
                            Member replyToAuthor = members.stream().filter(m -> m.getIdLong() == replyTo.getAuthor().getIdLong()).findFirst().orElse(null);
                            createAction.addComponents(ActionRow.of(Button.link(replyTo.getJumpUrl(), "↪ %s".formatted(MessageUtil.nameAndNickname(replyToAuthor, replyTo.getAuthor())))));
                        }

                        // Add embeds from the original message
                        if (!originalMessage.getEmbeds().isEmpty()) {
                            createAction.addEmbeds(originalMessage.getEmbeds());
                        }

                        // Download and add attachments from the original message
                        CompletableFuture<Void> attachmentsFuture =
                                originalMessage.getAttachments().stream()
                                        .map(attr -> attr.getProxy()
                                                             .download()
                                                             .thenApply(i -> FileUpload.fromData(i, attr.getFileName()))
                                                             .thenApply(file -> attr.isSpoiler() ? file.asSpoiler() : file)
                                                             .thenAccept(createAction::addFiles))
                                        .reduce(CompletableFuture.completedFuture(null), (a, b) -> a.thenCompose(_ -> b));
                        return attachmentsFuture.thenAccept(_ -> createAction.queue(onSuccess));
                    });
                });
    }

    /**
     * Retrieve or create new mirroring webhook in the current channel
     * @return retrieved or created {@link WebhookWithMessage}
     * */
    private CompletableFuture<WebhookWithMessage> getWebhook() {

        // Check if a webhook is already known for this channel
        if (CHANNEL_WEBHOOK.containsKey(channel.getIdLong())) {
            return CompletableFuture.completedFuture(CHANNEL_WEBHOOK.get(channel.getIdLong()));
        }

        IWebhookContainer webhookContainer = getiWebhookContainer();
        long selfId = Main.getJDA().getSelfUser().getIdLong();

        return webhookContainer
                       .retrieveWebhooks()
                       .submit()
                       .thenCompose(webhooks -> {
                           // Check if a webhook already exists in this channel
                           Optional<Webhook> webhookOpt =
                                   webhooks.stream()
                                           .filter(w -> w.getOwner() != null &&
                                                                w.getToken() != null &&
                                                                w.getOwner().getIdLong() == selfId)
                                           .findFirst();

                           // Webhook has been found => using existing one
                           if (webhookOpt.isPresent()) {
                               Webhook existing = webhookOpt.get();
                               WebhookWithMessage wh = new WebhookWithMessage(
                                       (WebhookImpl) existing,
                                       isThreadMirror(),
                                       getChannelId()
                               );
                               CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
                               return CompletableFuture.completedFuture(wh);
                           }

                           // Webhook has not been found => creating new one
                           return webhookContainer.createWebhook(DEFAULT_WEBHOOK_NAME)
                                          .submit()
                                          .thenApply(newWebhook -> {
                                              WebhookWithMessage wh = new WebhookWithMessage(
                                                      (WebhookImpl) newWebhook,
                                                      isThreadMirror(),
                                                      getChannelId()
                                              );
                                              CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
                                              return wh;
                                          });
                       });
    }

    /**
     * Retrieve the {@link IWebhookContainer} for the current channel
     * @return the {@link IWebhookContainer}
     */
    private IWebhookContainer getiWebhookContainer() {
        IWebhookContainer webhookContainer;
        if (channel instanceof ThreadChannel threadChanel) {
            if (!(threadChanel.getParentChannel() instanceof IWebhookContainer parentChannel))
                throw new IllegalStateException();
            webhookContainer = parentChannel;
        } else {
            if (!(channel instanceof IWebhookContainer webhookChannel)) throw new IllegalStateException();
            webhookContainer = webhookChannel;
        }
        return webhookContainer;
    }

    /**
     * Check if the mirroring is happening in a thread channel
     * @return true if the mirroring is happening in a thread channel, false otherwise
     * */
    boolean isThreadMirror() {
        return this.threadMirror;
    }

    /**
     * Get the ID of the channel where the mirroring is happening
     * @return the ID of the channel
     */
    long getChannelId() {
        return channel.getIdLong();
    }
}
