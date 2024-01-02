package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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

public class MirroredMessage {

    private static final Map<Long, WebhookWithMessage> CHANNEL_WEBHOOK = new HashMap<>();
    private static final String DEFAULT_WEBHOOK_NAME = "MIRROR_WEBHOOK";
    private final Message originalMessage;
    private Message mirrorMessage;
    private final TextChannel channel;
    private Message replyTo;
    private boolean mirror = false;
    private Consumer<Message> onceMessageSent;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    private final Map<Emoji, Integer> reactions = new HashMap<>();
    private String authorNameAndNickname;

    MirroredMessage(Message initialMessage, TextChannel textChannel) {
        this.channel = textChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        this.originalMessage = initialMessage;
        channel.getGuild().loadMembers().onSuccess(members -> authorNameAndNickname = MessageUtil.nameAndNickname(members, originalMessage.getAuthor()));
    }

    /**
     * Mirror {@link MirroredMessage#originalMessage} and run a {@link Consumer<Message>} once the mirror message has
     * been sent
     * @param replyTo the message {@link MirroredMessage#originalMessage} responded to if it an answer, can be null
     * @param onceMessageSent a {@link Consumer<Message>} to run once the mirror message has been sent passing the latter
     *                    as argument
     * */
    void mirrorMessage(@Nullable Message replyTo, Consumer<Message> onceMessageSent) {
        this.replyTo = replyTo;
        this.onceMessageSent = onceMessageSent;
        channel.getGuild().loadMembers().onSuccess(members -> checkBanTimeOut(originalMessage.getAuthor(), () -> createAndSendMessage(members)));
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
     * @param members a {@link List<Member>} of loaded server's members used to retrieve the authors's name of
     *                a potential replied message
     * */
    private void createAndSendMessage(List<Member> members) {
        String content = getContent(originalMessage);
        if(Main.getJDA().getSelfUser().getAvatar() == null) throw new IllegalStateException("Odds are not in our favor (should never arise)");

        // Retrieve author's profile picture or else defaulting on EPLBot profile picture
        Optional.ofNullable(originalMessage.getAuthor().getAvatar()).orElse(Main.getJDA().getSelfUser().getAvatar()).download().thenApply(is -> {
            try {
                return Icon.from(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(icon -> {
            WebhookMessageCreateAction<Message> createAction = getWebhook().sendMessage(authorNameAndNickname, icon, content);
            if (replyTo != null) {
                createAction.addComponents(ActionRow.of(Button.link(replyTo.getJumpUrl(), "↪ %s".formatted(MessageUtil.nameAndNickname(members, replyTo.getAuthor())))));
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
        if (content.isBlank()) {
            content = message.getEmbeds().isEmpty() ? "" : message.getEmbeds().get(0).getDescription();
        }
        return content;
    }

    /**
     * Retrieve or create new mirroring webhook in the current channel
     * @return retrieved or created {@link WebhookWithMessage}
     * */
    private WebhookWithMessage getWebhook() {
        List<Webhook> webhooks = channel.retrieveWebhooks().complete();

        // Check if a webhook is already known for this channel
        if(CHANNEL_WEBHOOK.containsKey(channel.getIdLong())) {
            return CHANNEL_WEBHOOK.get(channel.getIdLong());
        }

        // Check if a webhook already exists in this channel
        Optional<Webhook> webhookOpt = webhooks.stream().filter(w -> w.getOwner() != null && w.getOwner().getIdLong() == Main.getJDA().getSelfUser().getIdLong()).findFirst();
        if (webhookOpt.isPresent() && webhookOpt.get().getToken() != null) {
            WebhookWithMessage webhook = new WebhookWithMessage((WebhookImpl) webhookOpt.get());
            CHANNEL_WEBHOOK.put(channel.getIdLong(), webhook);
            return webhook;
        }

        // Webhook has not been found => creating new one
        Webhook webhook = channel.createWebhook(DEFAULT_WEBHOOK_NAME).complete();
        WebhookWithMessage wh = new WebhookWithMessage((WebhookImpl) webhook);
        CHANNEL_WEBHOOK.put(channel.getIdLong(), wh);
        return wh;
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
            this.mirror = true;
            updatePin(initialMessage.isPinned());
            onceMessageSent.accept(newMessage);
        });
    }

    void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());

        if (getWebhook() == null) return;
        if (!mirrorMessage.isWebhookMessage()) return;
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            if (!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated))
                return;

            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            getWebhook().editRequest(mirrorMessage.getId())
                    .setContent(content)
                    .setAttachments(attachments)
                    .queue(
                            m -> this.lastUpdated = m.getTimeEdited() == null ? m.getTimeCreated() : m.getTimeEdited(),
                            throwable -> {}
                    );
        });
    }

    private void updatePin(boolean pinned) {
        // Message#isPinned seems to be broken here
        mirrorMessage.getChannel().retrievePinnedMessages().map(l -> l.stream().map(ISnowflake::getIdLong).filter(id -> id == mirrorMessage.getIdLong()).findFirst()).queue(idOpt -> {
            if (pinned && idOpt.isEmpty())
                mirrorMessage.pin().queue();
            else if (!pinned && idOpt.isPresent())
                mirrorMessage.unpin().queue();
        });
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

    boolean isThreadOwner() {
        return this.threadOwner;
    }

    boolean isMirror() {
        return this.mirror;
    }

    void addReaction(MessageReaction reaction) {
        reactions.put(reaction.getEmoji(), reactions.getOrDefault(reaction.getEmoji(), 0) + 1);
        updateReactionField();
    }

    private void updateReactionField() {
        StringBuilder reactionString = new StringBuilder();
        List<Map.Entry<Emoji, Integer>> entries = new ArrayList<>(reactions.entrySet());
        for (int i = 0; i < entries.size() - 1; i++) {
            reactionString.append(entries.get(i).getKey().getFormatted()).append(": ").append(entries.get(i).getValue()).append(", ");
        }
        reactionString.append(entries.get(entries.size() - 1).getKey().getFormatted()).append(": ").append(entries.get(entries.size() - 1).getValue());

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

    void removeReaction(MessageReaction reaction) {
        reactions.computeIfPresent(reaction.getEmoji(), (e, i) -> i - 1);
        if (reactions.getOrDefault(reaction.getEmoji(), 1) <= 0) reactions.remove(reaction.getEmoji());
        updateReactionField();
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {

        @Override
        public String toString() {
            return "Tuple2{" +
                           "a=" + a +
                           ", b=" + b +
                           ", c=" + c +
                           '}';
        }

    }

}
