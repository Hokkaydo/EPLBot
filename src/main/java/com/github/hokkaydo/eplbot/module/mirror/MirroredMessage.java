package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
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
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.dv8tion.jda.api.entities.Message.MAX_CONTENT_LENGTH;

/**
 * Represents a message that is being mirrored from one channel to another using webhooks
 */
public class MirroredMessage {

    /** Map of channel IDs to their corresponding mirroring webhooks */
    private static final Map<Long, WebhookWithMessage> CHANNEL_WEBHOOK = new HashMap<>();
    /** Default name for the mirroring webhook */
    private static final String DEFAULT_WEBHOOK_NAME = "MIRROR_WEBHOOK";
    /** The original message to be mirrored */
    private final Message originalMessage;
    /** Channel where the mirroring is happening */
    private final GuildMessageChannel channel;
    /** Combined name and nickname of the author of the original message (e.g. "Hokkaydo (NicknameInTheMirrorGuild) */
    private final String authorNameAndNickname;
    /** Flag indicating if the mirroring is happening in a thread channel */
    private final boolean threadMirror;
    /** Map of member IDs to Members in the mirror guild */
    private final Map<Long, Member> mirrorMembers;
    /** The mirrored message in the target channel */
    private Message mirrorMessage;
    /** Timestamp of the last update made to the original message */
    private OffsetDateTime lastUpdated;
    /** Flag indicating if the mirrored message is owned by a thread */
    private boolean threadOwner;
    /** Flag indicating if the mirrored message is pinned */
    private boolean pinned = false;

    /** Create a new {@link MirroredMessage} instance
     * @param initialMessage the original message to mirror
     * @param textChannel the channel to mirror the message into
     * @param mirrorMembers the list of members in the mirror guild
     * */
    MirroredMessage(Message initialMessage, GuildMessageChannel textChannel, List<Member> mirrorMembers) {
        this.channel = textChannel;
        this.threadMirror = channel instanceof ThreadChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        this.originalMessage = initialMessage;
        this.mirrorMembers  = new HashMap<>();
        mirrorMembers.forEach(member -> this.mirrorMembers.put(member.getIdLong(), member));
        Member mirrorGuildMember = this.mirrorMembers.get(originalMessage.getAuthor().getIdLong());
        Member originalGuildMember = initialMessage.getMember();
        String prefix = "";
        if(isAssistant(mirrorGuildMember) || isAssistant(originalGuildMember))
            prefix = "[TA] ";
        this.authorNameAndNickname = prefix + MessageUtil.nameAndNickname(
                mirrorGuildMember,
                originalMessage.getAuthor()
        );
    }

    /**
     * Check if a given {@link Member} has the assistant role
     * @param member the member to check
     * @return true if the member has the assistant role, false otherwise
     * */
    private boolean isAssistant(Member member) {
        if(member == null) return false;
        String roleId = Config.getGuildVariable(member.getGuild().getIdLong(), "ASSISTANT_ROLE_ID");
        if(roleId.isBlank()) return false;
        return member.getRoles().stream().map(Role::getId).anyMatch(id -> id.equals(roleId));
    }

    /**
     * Mirror {@link MirroredMessage#originalMessage} and run a {@link Consumer<Message>} once the mirror message has
     * been sent
     * @param replyTo the message {@link MirroredMessage#originalMessage} responded to if it is an answer, can be null
     * @param onceMessageSent a {@link Consumer<Message>} to run once the mirror message has been sent passing the latter
     *                    as argument
     * */
    void mirrorMessage(@Nullable Message replyTo, Consumer<Message> onceMessageSent) {
        checkBanTimeOut(originalMessage.getAuthor(), () -> createAndSendMessage(onceMessageSent, replyTo));
    }

    /**
     * Check if a given {@link User} is currently not timed out before running the given {@link Runnable}
     * @param user the user to check time out for
     * @param notBanned the {@link Runnable} to run if the given user is not currently timed out
     * */
    private void checkBanTimeOut(User user, Runnable notBanned) {
        channel.getGuild().retrieveBanList().queue(list -> {
            if (list.stream().anyMatch(b -> b.getUser().getIdLong() == user.getIdLong())) return;
            Member authorMember = channel.getGuild().getMemberById(user.getIdLong());
            if (authorMember != null && (authorMember.isTimedOut())) return;
            notBanned.run();
        });
    }

    /**
     * Create the request to send a mirror message
     * @param onceMessageSent a {@link Consumer<Message>} to run once the mirror message has been sent passing the latter
     * as argument
     * @param replyTo the message {@link MirroredMessage#originalMessage} responded to if it is an answer, can be null
     * */
    private void createAndSendMessage(Consumer<Message> onceMessageSent, Message replyTo) {
        String content = getContent(originalMessage);

        originalMessage.getGuild().loadMembers().onSuccess(members -> {

            // Load icon of the author to give the webhook the same icon
            String iconUrl = Optional.ofNullable(originalMessage.getAuthor().getAvatar())
                                     .orElse(Main.getJDA().getSelfUser().getDefaultAvatar())
                                     .getUrl();

            // Determine denied mentions
            List<Message.MentionType> deniedMentions = new ArrayList<>(List.of(Message.MentionType.USER));
            Optional<Member> originalMember = members.stream().filter(m -> m.getIdLong() == originalMessage.getAuthor().getIdLong()).findFirst();
            if (originalMember.isEmpty() || !originalMember.get().hasPermission(Permission.MESSAGE_MENTION_EVERYONE)) {
                deniedMentions.addAll(List.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE));
            }

            List<Long> membersId = members.stream().map(Member::getIdLong).toList();

            getWebhook().thenCompose(webhook -> {
                WebhookMessageCreateAction<Message> createAction =
                        webhook.sendRequest()
                                .setContent(content)
                                .setAvatarUrl(iconUrl)
                                .setUsername(authorNameAndNickname)
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
                    Member replyToAuthor = mirrorMembers.get(replyTo.getAuthor().getIdLong());
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
                return attachmentsFuture.thenRun(() -> sendMessage(createAction, originalMessage, onceMessageSent));
            });
        });
    }

    /**
     * Retrieve the content of a given {@link Message}
     * @param message the message to retrieve text content from
     * @return message's content
     * */
    private String getContent(Message message) {
        String content = message.getContentRaw();
        return content.substring(0, Math.min(content.length(), MAX_CONTENT_LENGTH));
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
     * Execute a {@link WebhookMessageCreateAction<Message>} request and update the mirrored message once done
     * @param action the request to process
     * @param initialMessage the mirrored message
        * @param onceMessageSent a {@link Consumer<Message>} to run once the mirror message has been sent passing the latter
     * */
    private void sendMessage(WebhookMessageCreateAction<Message> action, Message initialMessage, Consumer<Message> onceMessageSent) {
        action.queue(newMessage -> {
            this.mirrorMessage = newMessage;
            updatePin(initialMessage.isPinned());
            onceMessageSent.accept(newMessage);
        });
    }

    /**
     * Retrieve the {@link IWebhookContainer} for the current channel
     * @return the {@link IWebhookContainer}
     */
    private IWebhookContainer getiWebhookContainer() {
        IWebhookContainer webhookContainer;
        if (channel instanceof ThreadChannel threadChanel) {
            if (!(threadChanel.getParentMessageChannel() instanceof IWebhookContainer parentChannel))
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

    /**
     * Update the pinned status of the mirrored message
     * @param shouldPin true if the mirrored message should be pinned, false otherwise
     */
    private void updatePin(boolean shouldPin) {
        if (!this.pinned && shouldPin) {
            (mirrorMessage == null ? originalMessage : mirrorMessage).pin().queue();
            this.pinned = true;
        }
        else if (this.pinned && !shouldPin) {
            (mirrorMessage == null ? originalMessage : mirrorMessage).unpin().queue();
            this.pinned = false;
        }
    }

    /**
     * Get the ID of the original message
     * @return the ID of the original message
     * */
    Long getOriginalMessageId() {
        return originalMessage.getIdLong();
    }

    /**
     * Get the ID of the mirrored message
     * @return the ID of the mirrored message
     */
    Long getMirrorMessageId() {
        return mirrorMessage.getIdLong();
    }

    /**
     * Delete the mirrored message
     */
    void delete() {
        mirrorMessage.delete().queue();
    }

    /**
     * Set the thread owner flag to true
     */
    void setThreadOwner() {
        this.threadOwner = true;
    }

    /**
     * Update the mirrored message if the original message has been edited
     * @param initialMessage the original message
     */
    void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());

        if(mirrorMessage == null) return;
        if (!mirrorMessage.isWebhookMessage()) return;
        if (getWebhook() == null) return;
        if (!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated)) return;
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            getWebhook().thenApply(webhook ->
                                           webhook.editRequest(mirrorMessage.getId())
                                                   .setContent(content)
                                                   .setAttachments(attachments)
            ).thenAccept(action -> action.queue(m -> this.lastUpdated = m.getTimeEdited() == null ? m.getTimeCreated() : m.getTimeEdited()));
        });
    }

    /**
     * Check if the current mirrored message is owned by a thread
     * @return true if the current mirrored message is owned by a thread, false otherwise
     */
    boolean isThreadOwner() {
        return this.threadOwner;
    }

    /**
     * Check if the mirroring has been completed
     * @return true if the mirroring has been completed, false otherwise
     */
    boolean isMirror() {
        return this.mirrorMessage != null;
    }
}
