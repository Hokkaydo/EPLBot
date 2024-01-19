package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.entities.WebhookImpl;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.dv8tion.jda.api.entities.Message.MAX_CONTENT_LENGTH;

public class MirroredMessage {

    private static final Map<Long, WebhookWithMessage> CHANNEL_WEBHOOK = new HashMap<>();
    private static final String DEFAULT_WEBHOOK_NAME = "MIRROR_WEBHOOK";
    private final Message originalMessage;
    private Message mirrorMessage;
    private final GuildMessageChannel channel;
    private Message replyTo;
    private Consumer<Message> onceMessageSent;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    private final Map<Emoji, Integer> reactions = new HashMap<>();
    private final String authorNameAndNickname;
    private boolean pinned = false;
    private final boolean threadMirror;
    private final Map<Long, Member> mirrorMembers;

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
        this.replyTo = replyTo;
        this.onceMessageSent = onceMessageSent;

        checkBanTimeOut(originalMessage.getAuthor(), this::createAndSendMessage);
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
     * */
    private void createAndSendMessage() {
        String content = getContent(originalMessage);
        // Retrieve author's profile picture or else defaulting on default profile picture
        Optional.ofNullable(originalMessage.getAuthor().getAvatar()).orElse(Main.getJDA().getSelfUser().getDefaultAvatar()).download().thenApply(is -> {
            try {
                return Icon.from(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(icon -> {
            WebhookMessageCreateAction<Message> createAction = getWebhook().sendMessage(authorNameAndNickname, icon, content, originalMessage.getAuthor().getIdLong());
            if (replyTo != null) {
                Member replyToAuthor = mirrorMembers.get(replyTo.getAuthor().getIdLong());
                createAction.addComponents(ActionRow.of(Button.link(replyTo.getJumpUrl(), "↪ %s".formatted(MessageUtil.nameAndNickname(replyToAuthor, replyTo.getAuthor())))));
            }
            if (!originalMessage.getEmbeds().isEmpty()) {
                createAction.addEmbeds(originalMessage.getEmbeds());
            }


            AtomicReference<WebhookMessageCreateAction<Message>> action = new AtomicReference<>(createAction);
            originalMessage.getAttachments().stream()
                    .map(m -> new Tuple3<>(m.getFileName(), m.getProxy().download(), m.isSpoiler()))
                    .map(tuple3 -> tuple3.b()
                                           .thenApply(i -> FileUpload.fromData(i, tuple3.a()))
                                           .thenApply(f -> Boolean.TRUE.equals(tuple3.c()) ? f.asSpoiler() : f)
                    )
                    .map(c -> c.thenAccept(f -> action.get().addFiles(f)))
                    .reduce((a, b) -> {
                        a.join();
                        return b;
                    })
                    .ifPresentOrElse(
                            c -> {
                                c.join();
                                sendMessage(action.get(), originalMessage);
                            },
                            () -> sendMessage(action.get(), originalMessage));
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
    private WebhookWithMessage getWebhook() {

        // Check if a webhook is already known for this channel
        if (CHANNEL_WEBHOOK.containsKey(channel.getIdLong())) {
            return CHANNEL_WEBHOOK.get(channel.getIdLong());
        }
        IWebhookContainer webhookContainer = getiWebhookContainer();

        // Check if a webhook already exists in this channel
        List<Webhook> webhooks = webhookContainer.retrieveWebhooks().complete();
        Optional<Webhook> webhookOpt = webhooks.stream().filter(w -> w.getOwner() != null && w.getOwner().getIdLong() == Main.getJDA().getSelfUser().getIdLong()).findFirst();
        if (webhookOpt.isPresent() && webhookOpt.get().getToken() != null) {
            WebhookWithMessage webhook = new WebhookWithMessage((WebhookImpl) webhookOpt.get(), isThreadMirror(), getChannelId());
            CHANNEL_WEBHOOK.put(channel.getIdLong(), webhook);
            return webhook;
        }

        // Webhook has not been found => creating new one
        Webhook webhook = webhookContainer.createWebhook(DEFAULT_WEBHOOK_NAME).complete();
        WebhookWithMessage wh = new WebhookWithMessage((WebhookImpl) webhook, isThreadMirror(), getChannelId());
        CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
        return wh;
    }

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
     * Execute a {@link WebhookMessageCreateAction<Message>} request and passes the result to
     * {@link MirroredMessage#onceMessageSent}
     * @param action the request to process
     * @param initialMessage the mirrored message
     * */
    private void sendMessage(WebhookMessageCreateAction<Message> action, Message initialMessage) {
        action.queue(newMessage -> {
            this.mirrorMessage = newMessage;
            updatePin(initialMessage.isPinned());
            onceMessageSent.accept(newMessage);
        });
    }

    boolean isThreadMirror() {
        return this.threadMirror;
    }

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

    Long getOriginalMessageId() {
        return originalMessage.getIdLong();
    }

    Long getMirrorMessageId() {
        return mirrorMessage.getIdLong();
    }

    void delete() {
        mirrorMessage.delete().queue();
    }

    long getChannelId() {
        return channel.getIdLong();
    }

    void setThreadOwner() {
        this.threadOwner = true;
    }

    void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());

        if(mirrorMessage == null) return;
        if (!mirrorMessage.isWebhookMessage()) return;
        if (getWebhook() == null) return;
        if (!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated)) return;
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            getWebhook().editRequest(mirrorMessage.getId())
                    .setContent(content)
                    .setAttachments(attachments)
                    .queue(
                            m -> this.lastUpdated = m.getTimeEdited() == null ? m.getTimeCreated() : m.getTimeEdited(),
                            _ -> {}
                    );
        });
    }

    boolean isThreadOwner() {
        return this.threadOwner;
    }

    boolean isMirror() {
        return this.mirrorMessage != null;
    }


    /**
     * @deprecated no viable way to add reactions to messages
     * */
    @SuppressWarnings("unused")
    @Deprecated
    void addReaction(MessageReaction reaction) {
        reactions.put(reaction.getEmoji(), reactions.getOrDefault(reaction.getEmoji(), 0) + 1);
        updateReactionField();
    }

    /**
     * @deprecated no viable way to add reactions to messages
     * */
    @Deprecated
    private void updateReactionField() {
        StringBuilder reactionString = new StringBuilder();
        List<Map.Entry<Emoji, Integer>> entries = new ArrayList<>(reactions.entrySet());
        for (int i = 0; i < entries.size() - 1; i++) {
            reactionString.append(entries.get(i).getKey().getFormatted()).append(": ").append(entries.get(i).getValue()).append(", ");
        }
        reactionString.append(entries.getLast().getKey().getFormatted()).append(": ").append(entries.getLast().getValue());

        Optional<MessageEmbed> oldEmbed = originalMessage.getEmbeds().stream().filter(e -> e.getType() == EmbedType.RICH).findFirst();
        if (oldEmbed.isEmpty()) return;
        List<MessageEmbed.Field> otherFields = new ArrayList<>(oldEmbed.get().getFields().stream().filter(f -> f.getName() == null || !f.getName().equals("Réactions")).toList());
        EmbedBuilder newEmbed = new EmbedBuilder(oldEmbed.get()).clearFields();
        otherFields.add(new MessageEmbed.Field("Réactions", reactionString.toString(), true));
        for (MessageEmbed.Field otherField : otherFields) {
            newEmbed.addField(otherField);
        }
        List<MessageEmbed> otherEmbeds = new ArrayList<>(originalMessage.getEmbeds().stream().filter(e -> e.getType() != EmbedType.RICH).toList());
        otherEmbeds.add(newEmbed.build());
        originalMessage.editMessageEmbeds(otherEmbeds).queue();
    }

    /**
     * @deprecated no viable way to add reactions to messages
     * */
    @SuppressWarnings("unused")
    @Deprecated
    void removeReaction(MessageReaction reaction) {
        reactions.computeIfPresent(reaction.getEmoji(), (e, i) -> i - 1);
        if (reactions.getOrDefault(reaction.getEmoji(), 1) <= 0) reactions.remove(reaction.getEmoji());
        updateReactionField();
    }


    private record Tuple3<A, B, C>(A a, B b, C c) {

        @Override
        public String toString() {
            return STR."Tuple2{a=\{a}, b=\{b}, c=\{c}\{'}'}";
        }

    }

}
