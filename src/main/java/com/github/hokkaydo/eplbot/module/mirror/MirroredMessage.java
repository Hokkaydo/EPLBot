package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class MirroredMessage {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*$");

    private Message message;
    private final MessageChannel channel;
    private boolean embed;
    private OffsetDateTime lastUpdated;

    public MirroredMessage(Message initialMessage, MessageChannel textChannel, boolean mirror) {
        this.channel = textChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        if(mirror) {
            mirrorMessage(initialMessage);
        } else {
            this.message = initialMessage;
        }
        this.embed = mirror;
    }

    public void mirrorMessage(Message initialMessage) {

        MessageCreateAction createAction;
        String content = getContent(initialMessage);
        if(Arrays.stream(content.split(" ")).anyMatch(s -> URL_PATTERN.matcher(s).find()))
            createAction = channel.sendMessage(initialMessage.getContentRaw());
        else
            createAction = channel.sendMessageEmbeds(MessageUtil.toEmbed(initialMessage).build());
        AtomicReference<MessageCreateAction> action = new AtomicReference<>(createAction);
        initialMessage.getAttachments().stream()
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
                            sendMessage(action, initialMessage);
                        },
                        () -> sendMessage(action, initialMessage));
    }

    private void sendMessage(AtomicReference<MessageCreateAction> action, Message initialMessage) {
        this.message = action.get().complete();
        updatePin(initialMessage.isPinned());
    }

    private String getContent(Message message) {
        String content = message.getContentRaw().isEmpty() ? (message.getEmbeds().isEmpty() ? "" : message.getEmbeds().get(0).getDescription()) : message.getContentRaw();
        return content == null ? "" : content;
    }

    public void update(Message initialMessage) {
        if(!(initialMessage.getTimeEdited() == null ? initialMessage.getTimeCreated() : initialMessage.getTimeEdited()).isAfter(lastUpdated)) return;
        String content = getContent(initialMessage);
        List<Message.Attachment> attachments = initialMessage.getAttachments();
        boolean embed = Arrays.stream(content.split(" ")).noneMatch(s -> URL_PATTERN.matcher(s).find());
        if(message.getAuthor().getIdLong() == Main.getJDA().getSelfUser().getIdLong()) {
            if (this.embed) {
                if (embed) {
                    MessageEmbed messageEmbed = message.getEmbeds().get(0);
                    if (messageEmbed == null) return;
                    EmbedBuilder newEmbed = new EmbedBuilder(messageEmbed).setDescription(content);
                    message.editMessageEmbeds(newEmbed.build()).setAttachments(attachments).queue();
                } else {
                    message.suppressEmbeds(true).map(v -> message.editMessage(content).setAttachments(attachments)).queue();
                    this.embed = false;
                }
            } else {
                if (embed) {
                    message.editMessageEmbeds(initialMessage.getEmbeds().isEmpty() ? MessageUtil.toEmbed(initialMessage).build() : initialMessage.getEmbeds().get(0)).setContent("").setAttachments(attachments).queue();
                    this.embed = true;
                } else {
                    message.editMessage(content).setAttachments(attachments).queue();
                }
            }
        }
        updatePin(initialMessage.isPinned());
        this.lastUpdated = message.getTimeEdited() == null ? message.getTimeCreated() : message.getTimeEdited();
    }

    private void updatePin(boolean pinned) {
        if(pinned && !message.isPinned())
            message.pin().queue();
        else if (!pinned && message.isPinned())
            message.unpin().queue();
    }

    public Long getMessageId() {
        return message.getIdLong();
    }

    public void delete() {
        message.delete().queue();
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}


}
