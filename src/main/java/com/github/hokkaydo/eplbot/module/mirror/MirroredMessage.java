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
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.WebhookManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ImageProxy;
import net.dv8tion.jda.internal.entities.WebhookImpl;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public class MirroredMessage {

    private final Message originalMessage;
    private Message mirrorMessage;
    private boolean mirror = false;
    private final GuildMessageChannel channel;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    private final Map<Emoji, Integer> reactions = new HashMap<>();
    private WebhookWithMessage webhook;

    MirroredMessage(Message initialMessage, GuildMessageChannel textChannel) {
        this.channel = textChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        this.originalMessage = initialMessage;
    }

    private void checkBanTimeOut(User user, Runnable runnable) {
        channel.getGuild().retrieveBanList().queue(list -> {
            if (list.stream().anyMatch(b -> b.getUser().getIdLong() == user.getIdLong())) return;
            Member authorMember = channel.getGuild().getMemberById(user.getIdLong());
            if (authorMember != null && (authorMember.isTimedOut())) return;
            runnable.run();
        });
    }

    void mirrorMessage(Message replyTo, Consumer<Message> sentMessage) {
        TextChannel textChannel = Main.getJDA().getTextChannelById(channel.getId());
        if (textChannel == null) return;
        channel.getGuild().loadMembers().onSuccess(members -> cleanWebhooks(
                textChannel,
                members,
                () -> createWebhook(textChannel, members, () -> checkBanTimeOut(originalMessage.getAuthor(), () -> createAndSendMessage(replyTo, members, sentMessage))),
                () -> checkBanTimeOut(originalMessage.getAuthor(), () -> createAndSendMessage(replyTo, members, sentMessage))
        ));
    }

    private void createAndSendMessage(Message replyTo, List<Member> members, Consumer<Message> sentMessage) {
        WebhookMessageCreateAction<Message> createAction;
        String content = getContent(originalMessage);
        createAction = this.webhook.sendMessage(content);
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
                            sendMessage(action.get(), originalMessage, sentMessage);
                        },
                        () -> sendMessage(action.get(), originalMessage, sentMessage));
    }

    private void cleanWebhooks(TextChannel channel, List<Member> members, Runnable noSimilarWebhookFound, Runnable similarWebhookFound) {
        channel.retrieveWebhooks().queue(webhooks -> {
            Optional<Webhook> webhookOpt = webhooks.stream().sorted(Comparator.comparing(ISnowflake::getTimeCreated).reversed()).filter(w -> w.getName().equals(MessageUtil.nameAndNickname(members, originalMessage.getAuthor()))).findFirst();
            if (webhookOpt.isPresent() && webhookOpt.get().getToken() != null) {
                this.webhook = new WebhookWithMessage((WebhookImpl) webhookOpt.get());
                similarWebhookFound.run();
                return;
            }
            if (webhooks.size() >= 10)
                webhooks.stream().filter(w -> w.getOwner() != null && w.getOwner().getIdLong() == Main.getJDA().getSelfUser().getIdLong())
                        .sorted(Comparator.comparing(ISnowflake::getTimeCreated))
                        .limit(5)
                        .map(Webhook::delete)
                        .forEach(RestAction::queue);
            noSimilarWebhookFound.run();
        });
    }

    private void createWebhook(TextChannel textChannel, List<Member> members, Runnable then) {
        ImageProxy avatarProxy = originalMessage.getAuthor().getAvatar();
        CompletableFuture<InputStream> avatarFuture = avatarProxy == null ? CompletableFuture.completedFuture(null) : avatarProxy.download();
        textChannel.createWebhook(MessageUtil.nameAndNickname(members, originalMessage.getAuthor())).queue(w -> avatarFuture.thenAccept(is -> {
            try {
                WebhookManager manager = w.getManager().setName(MessageUtil.nameAndNickname(members, originalMessage.getAuthor())).setChannel((TextChannel) channel);
                if (is != null)
                    manager = manager.setAvatar(Icon.from(is));
                manager.queue();
                this.webhook = new WebhookWithMessage((WebhookImpl) w);
                MirrorModule.getExecutorService().schedule(() -> {
                    w.delete().queue();
                    this.webhook = null;
                }, 10L, TimeUnit.MINUTES);
                then.run();
            } catch (IOException e) {
                Main.LOGGER.log(Level.WARNING, "Could not send webhook files");
            }
        }));
    }

    private void sendMessage(WebhookMessageCreateAction<Message> messageToSend, Message initialMessage, Consumer<Message> sentMessage) {
        messageToSend.queue(newMessage -> {
            this.mirrorMessage = newMessage;
            this.mirror = true;
            updatePin(initialMessage.isPinned());
            sentMessage.accept(newMessage);
        });
    }

    private String getContent(Message message) {
        String content = message.getContentRaw();
        if (content.isBlank()) {
            content = message.getEmbeds().isEmpty() ? "" : message.getEmbeds().get(0).getDescription();
        }
        return content;
    }

    void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());


        if (this.webhook == null) return;
        if (!mirrorMessage.isWebhookMessage()) return;
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            if (!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated))
                return;

            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            webhook.editRequest(mirrorMessage.getId())
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
