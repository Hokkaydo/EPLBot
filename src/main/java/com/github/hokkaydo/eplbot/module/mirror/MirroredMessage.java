package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MirroredMessage {

    private Message message;
    private final MessageChannel channel;
    private OffsetDateTime lastUpdated;
    private boolean threadOwner;
    public MirroredMessage(Message initialMessage, MessageChannel textChannel, boolean mirror) {
        this.channel = textChannel;
        this.lastUpdated = initialMessage.getTimeCreated();
        if(mirror) {
            mirrorMessage(initialMessage);
        } else {
            this.message = initialMessage;
        }
    }

    public void mirrorMessage(Message initialMessage) {
        MessageCreateAction createAction;
        String content = getContent(initialMessage);
        createAction = channel.sendMessageEmbeds(MessageUtil.toEmbed(initialMessage).setDescription(content).setFooter("").build());
        if(!initialMessage.getEmbeds().isEmpty()) {
            createAction.addEmbeds(initialMessage.getEmbeds());
        }
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
        action.get().queue(m -> {
            this.message = m;
            updatePin(initialMessage.isPinned());
        });
    }

    private String getContent(Message message) {
        String content = message.getContentRaw().isEmpty() ? "" : message.getContentRaw();
        if(content.isBlank()) {
            content = message.getEmbeds().isEmpty() ? "" : message.getEmbeds().get(0).getDescription();
        }
        return content == null ? "" : content;
    }

    public void update(Message initialMessage) {
        updatePin(initialMessage.isPinned());
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
    }

    private void updatePin(boolean pinned) {
        if(pinned && !message.isPinned())
            message.pin().queue();
        else if (!pinned && message.isPinned())
            message.unpin().queue();
    }

    public Long getMessageId() {
        return message == null ? 0 : message.getIdLong();
    }

    public void delete() {
        message.delete().queue();
    }

    public long getChannelId() {
        return channel.getIdLong();
    }

    public void setThreadOwner(boolean threadOwner) {
        this.threadOwner = threadOwner;
    }

    public boolean isThreadOwner() {
        return this.threadOwner;
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}


}
