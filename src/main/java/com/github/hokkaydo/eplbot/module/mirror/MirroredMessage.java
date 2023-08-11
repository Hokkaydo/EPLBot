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

    private Message message;
    private final GuildMessageChannel channel;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    private final Map<Emoji, Integer> reactions = new HashMap<>();
    private WebhookWithMessage webhook;
    MirroredMessage(Message initialMessage, GuildMessageChannel textChannel) {
        this.channel = textChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        this.message = initialMessage;
    }

    private void checkBanTimeOut(User user, Runnable runnable) {
        channel.getGuild().retrieveBanList().queue(list -> {
            if(list.stream().anyMatch(b -> b.getUser().getIdLong() == user.getIdLong())) return;
            Member authorMember = channel.getGuild().getMemberById(user.getIdLong());
            if(authorMember != null && (authorMember.isTimedOut())) return;
            runnable.run();
        });
    }

    void mirrorMessage(Message replyTo, Consumer<Message> sentMessage) {
        TextChannel textChannel = Main.getJDA().getTextChannelById(channel.getId());
        if(textChannel == null) return;
        cleanWebhooks(textChannel, () -> createWebhook(textChannel, members -> checkBanTimeOut(message.getAuthor(), ()-> createAndSendMessage(replyTo, members, sentMessage))));
    }

    private void createAndSendMessage(Message replyTo, List<Member> members, Consumer<Message> sentMessage) {
        WebhookMessageCreateAction<Message> createAction;
        String content = getContent(message);
        createAction = this.webhook.sendMessage(content);
        if(replyTo != null) {
            createAction.addComponents(ActionRow.of(Button.link(replyTo.getJumpUrl(), "↪ %s".formatted(MessageUtil.nameAndNickname(members, replyTo.getAuthor())))));
        }
        if(!message.getEmbeds().isEmpty()) {
            createAction.addEmbeds(message.getEmbeds());
        }
        AtomicReference<WebhookMessageCreateAction<Message>> action = new AtomicReference<>(createAction);
        message.getAttachments().stream()
                .map(m -> new Tuple3<>(m.getFileName(), m.getProxy().download(), m.isSpoiler()))
                .map(tuple3 -> tuple3.b()
                                       .thenApply(i -> FileUpload.fromData(i, tuple3.a()))
                                       .thenApply(f -> Boolean.TRUE.equals(tuple3.c()) ? f.asSpoiler() : f)
                )
                .map(c -> c.thenAccept(f -> action.get().addFiles(f)))
                .reduce((a,b) -> {a.join(); return b;})
                .ifPresentOrElse(
                        c -> {
                            c.join();
                            sendMessage(action.get(), message, sentMessage);
                        },
                        () -> sendMessage(action.get(), message, sentMessage));
    }

    private void createWebhook(TextChannel textChannel, Consumer<List<Member>> then) {
        ImageProxy avatarProxy = message.getAuthor().getAvatar();
        CompletableFuture<InputStream> avatarFuture = avatarProxy == null ? CompletableFuture.completedFuture(null) : avatarProxy.download();
        channel.getGuild().loadMembers().onSuccess(members -> textChannel.createWebhook(message.getAuthor().getName()).queue(w -> avatarFuture.thenAccept(is -> {
            try {
                WebhookManager manager = w.getManager().setName(MessageUtil.nameAndNickname(members, message.getAuthor())).setChannel((TextChannel) channel);
                if (is != null)
                    manager = manager.setAvatar(Icon.from(is));
                manager.queue();
                this.webhook = new WebhookWithMessage((WebhookImpl) w);
                MirrorModule.getExecutorService().schedule(() -> {
                    w.delete().queue();
                    this.webhook = null;
                }, 10L, TimeUnit.MINUTES);
                then.accept(members);
            } catch (IOException e) {
                Main.LOGGER.log(Level.WARNING, "Could not send webhook files");
            }
        })));
    }

    private void cleanWebhooks(TextChannel channel, Runnable then) {
        channel.retrieveWebhooks().queue(webhooks -> {
            if(webhooks.size() >= 10)
                webhooks.stream().filter(w -> w.getOwner() != null && w.getOwner().getIdLong() == Main.getJDA().getSelfUser().getIdLong())
                        .sorted(Comparator.comparing(ISnowflake::getTimeCreated))
                        .limit(5)
                        .map(Webhook::delete)
                        .forEach(RestAction::queue);
            then.run();
        });
    }

    private void sendMessage(WebhookMessageCreateAction<Message> messageToSend, Message initialMessage, Consumer<Message> sentMessage) {
        messageToSend.queue(newMessage -> {
            this.message = newMessage;
            updatePin(initialMessage.isPinned());
            sentMessage.accept(newMessage);
        });
    }

    private String getContent(Message message) {
        String content = message.getContentRaw();
        if(content.isBlank()) {
            content = message.getEmbeds().isEmpty() ? "" : message.getEmbeds().get(0).getDescription();
        }
        return content;
    }

    void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());
        if(this.webhook == null) return;
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            if(!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated)) return;

            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            if(!message.isWebhookMessage()) return;
            webhook.editRequest(message.getId()).setContent(content).setAttachments(attachments).queue(
                    m -> this.lastUpdated = m.getTimeEdited() == null ? m.getTimeCreated() : m.getTimeEdited(),
                    throwable -> {}
            );
        });
    }

    private void updatePin(boolean pinned) {
        // Message#isPinned seems to be broken here
        message.getChannel().retrievePinnedMessages().map(l -> l.stream().map(ISnowflake::getIdLong).filter(id -> id == message.getIdLong()).findFirst()).queue(idOpt -> {
            if(pinned && idOpt.isEmpty())
                message.pin().queue();
            else if (!pinned && idOpt.isPresent())
                message.unpin().queue();
        });
    }

    Long getMessageId() {
        return message == null ? 0 : message.getIdLong();
    }

    void delete() {
        message.delete().queue();
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

        Optional<MessageEmbed> oldEmbed = message.getEmbeds().stream().filter(e -> e.getType() == EmbedType.RICH).findFirst();
        if(oldEmbed.isEmpty()) return;
        List<MessageEmbed.Field> otherFields = new ArrayList<>(oldEmbed.get().getFields().stream().filter(f -> f.getName() == null || !f.getName().equals("Réactions")).toList());
        EmbedBuilder newEmbed = new EmbedBuilder(oldEmbed.get()).clearFields();
        otherFields.add(new MessageEmbed.Field("Réactions", reactionString.toString(), true));
        for (MessageEmbed.Field otherField : otherFields) {
            newEmbed.addField(otherField);
        }
        List<MessageEmbed> otherEmbeds = new ArrayList<>(message.getEmbeds().stream().filter(e -> e.getType() != EmbedType.RICH).toList());
        otherEmbeds.add(newEmbed.build());
        message.editMessageEmbeds(otherEmbeds).queue();
    }

    void removeReaction(MessageReaction reaction) {
        reactions.computeIfPresent(reaction.getEmoji(), (e, i) -> i-1);
        if(reactions.getOrDefault(reaction.getEmoji(), 1) <= 0) reactions.remove(reaction.getEmoji());
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
