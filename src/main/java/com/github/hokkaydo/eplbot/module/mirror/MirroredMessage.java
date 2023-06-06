package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

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

    private Message message;
    private final GuildMessageChannel channel;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    private final Map<Emoji, Integer> reactions = new HashMap<>();
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
        Main.getJDA().getTextChannelById(channel.getIdLong()).createWebhook("").queue(w -> {
            message.getAuthor().getAvatar().download().thenAccept(is -> {
                Member authorMember = channel.getGuild().getMemberById(message.getAuthor().getIdLong());
                boolean hasNickname = authorMember != null && authorMember.getNickname() != null;
                String authorNickAndTag = (hasNickname  ? authorMember.getNickname() + " (" : "") + message.getAuthor().getAsTag() + (hasNickname ? ")" : "");
                try {
                    w.getManager().setAvatar(Icon.from(is)).setName(authorNickAndTag).queue();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        checkBanTimeOut(message.getAuthor(), () -> {
            MessageCreateAction createAction;
            String content = getContent(message);
            Member authorMember = channel.getGuild().getMemberById(message.getAuthor().getIdLong());
            boolean hasNickname = authorMember != null && authorMember.getNickname() != null;
            String authorNickAndTag = (hasNickname  ? authorMember.getNickname() + " (" : "") + message.getAuthor().getName() + (hasNickname ? ")" : "");
            MessageEmbed embed = MessageUtil.toEmbed(message)
                                         .setAuthor(authorNickAndTag, message.getJumpUrl(), message.getAuthor().getAvatarUrl())
                                         .setDescription(content)
                                         .setFooter("")
                                         .setTimestamp(null)
                                         .build();
            if(replyTo != null)
                createAction = replyTo.replyEmbeds(embed);
            else
                createAction = channel.sendMessageEmbeds(embed);
            if(!message.getEmbeds().isEmpty()) {
                createAction.addEmbeds(message.getEmbeds());
            }
            AtomicReference<MessageCreateAction> action = new AtomicReference<>(createAction);
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
                                sendMessage(action, message, sentMessage);
                            },
                            () -> sendMessage(action, message, sentMessage));
        });
    }

    private void sendMessage(AtomicReference<MessageCreateAction> action, Message initialMessage, Consumer<Message> sentMessage) {
        action.get().queue(m -> {
            this.message = m;
            updatePin(initialMessage.isPinned());
            sentMessage.accept(m);
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
        checkBanTimeOut(initialMessage.getAuthor(), () -> {
            if(!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated)) return;
            String content = getContent(initialMessage);
            List<Message.Attachment> attachments = initialMessage.getAttachments();
            if(message.getAuthor().getIdLong() == Main.getJDA().getSelfUser().getIdLong()) {
                Optional<MessageEmbed> oldEmbed = message.getEmbeds().stream().filter(e -> e.getType() == EmbedType.RICH).findFirst();
                if(oldEmbed.isEmpty()) return;
                MessageEmbed newEmbed = new EmbedBuilder(oldEmbed.get()).setDescription(content).build();
                List<MessageEmbed> current = new java.util.ArrayList<>(initialMessage.getEmbeds().stream().filter(e -> e.getType() != EmbedType.RICH).toList());
                current.add(newEmbed);
                message.editMessageEmbeds(current).map(m -> m.editMessageAttachments(attachments)).queue();
            }
            updatePin(initialMessage.isPinned());
            this.lastUpdated = message.getTimeEdited() == null ? message.getTimeCreated() : message.getTimeEdited();
        });
    }

    private void updatePin(boolean pinned) {
        if(pinned && !message.isPinned())
            message.pin().queue();
        else if (!pinned && message.isPinned())
            message.unpin().queue();
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
            return "Tuple3{" +
                           "a=" + a +
                           ", b=" + b +
                           ", c=" + c +
                           '}';
        }

    }


}
