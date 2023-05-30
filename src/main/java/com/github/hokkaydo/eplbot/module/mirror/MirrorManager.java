package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MirrorManager extends ListenerAdapter {

    private static final Path MIRROR_STORAGE_PATH = Path.of(Main.PERSISTENCE_DIR_PATH + "/mirrors");
    private final List<Mirror> mirrors = new ArrayList<>();
    private final List<MirroredMessages> mirroredMessages = new ArrayList<>();

    public void createLink(GuildMessageChannel first, GuildMessageChannel second) {
        if(existsLink(first, second)) return;
        mirrors.add(new Mirror(first, second));
        storeMirrors();
    }

    public List<Mirror> getLinks(GuildMessageChannel idLong) {
        return mirrors.stream().filter(m -> m.has(idLong)).toList();
    }

    public Optional<Mirror> getLink(GuildMessageChannel first, GuildMessageChannel second) {
        return mirrors.stream()
                       .filter(mirror -> (mirror.first.equals(first) && mirror.second.equals(second)) ||
                                                 (mirror.second.equals(first) && mirror.first.equals(second)))
                       .findFirst();
    }

    public boolean existsLink(GuildMessageChannel first, GuildMessageChannel second) {
        return getLink(first, second).isPresent();
    }

    public void destroyLink(GuildMessageChannel first, GuildMessageChannel second) {
        getLink(first, second).ifPresent(mirrors::remove);
        storeMirrors();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getMessage().isEphemeral()) return;
        if(event.getMessage().getType().isSystem()) return;
        if(event.getMessage().getAuthor().getIdLong() == Main.getJDA().getSelfUser().getIdLong()) return;
        if(mirroredMessages.stream().flatMap(m -> m.getMessages().stream()).anyMatch(m -> m.getMessageId() == 0 || m.getMessageId() == event.getMessageIdLong())) return;
        if(event.getMessage().getType().equals(MessageType.THREAD_STARTER_MESSAGE) || event.getMessage().getType().equals(MessageType.THREAD_CREATED)) {
            ThreadChannel threadChannel = event.getMessage().getChannel().asThreadChannel();
            threadChannel.retrieveParentMessage().queue(parent -> mirrors.stream().filter(m -> m.has(event.getChannel().asThreadChannel().getParentMessageChannel())).forEach(mirror -> {
                GuildMessageChannel other = mirror.other(parent.getChannel().asGuildMessageChannel());
                mirroredMessages.stream()
                        .filter(m -> m.getMessages().stream().anyMatch(msg -> msg.getMessageId() == parent.getIdLong()))
                        .filter(m -> m.getMessages().stream().anyMatch(msg -> msg.getChannelId() == other.getIdLong()))
                        .flatMap(m -> m.getMessages().stream().filter(msg -> msg.getChannelId() == other.getIdLong()))
                        .filter(m -> !m.isThreadOwner())
                        .findFirst()
                        .ifPresentOrElse(message -> {
                            createThread(message.getMessageId(), other.getIdLong(), threadChannel);
                            message.setThreadOwner(true);
                        }, () -> createThread(other.getLatestMessageIdLong(), other.getIdLong(), threadChannel));
            }));
            return;
        }
        GuildMessageChannel originalChannel = event.getChannel().asGuildMessageChannel();
        boolean reply = event.getMessage().getType().equals(MessageType.INLINE_REPLY) && event.getMessage().getReferencedMessage() != null;
        MirroredMessage initial = new MirroredMessage(event.getMessage(), originalChannel, false, event.getMessage().getReferencedMessage());
        MirroredMessages messages = new MirroredMessages(initial, new ArrayList<>());
        mirrors.stream().filter(m -> m.has(originalChannel)).forEach(mirror -> {
            GuildMessageChannel other = mirror.other(originalChannel);
            if(reply) {
                mirroredMessages.stream()
                        .filter(m -> m.match(event.getMessage().getReferencedMessage().getIdLong()))
                        .findFirst()
                        .flatMap(message -> message.getMessages().stream().filter(m -> m.getChannelId() == other.getIdLong()).findFirst())
                        .map(m -> new Tuple2<>(m, Main.getJDA().getChannelById(GuildMessageChannel.class, m.getChannelId())))
                        .map(t -> new Tuple2<>(t.a, t.b.retrieveMessageById(t.a.getMessageId())))
                        .map(Tuple2::b)
                        .ifPresentOrElse(a -> a.queue(replyMsg -> {
                            MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other, true, replyMsg);
                            messages.mirrored.add(mirroredMessage);
                        }), () -> {
                            MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other, true, null);
                            messages.mirrored.add(mirroredMessage);
                        });
            }
            else {
                MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other, true, null);
                messages.mirrored.add(mirroredMessage);
            }
        });
        mirroredMessages.add(messages);
    }

    private record Tuple2<A, B>(A a, B b){}

    private void createThread(Long messageId, Long channelId, ThreadChannel firstThread) {
        TextChannel channel = Main.getJDA().getChannelById(TextChannel.class, channelId);
        if(channel == null) return;
        channel.retrieveMessageById(messageId).queue(m -> {
            if(m.getStartedThread() != null) return;
            m.createThreadChannel(firstThread.getName()).queue(t -> {
                MessageUtil.sendWarning(Strings.getString("THREAD_FIRST_MESSAGE_NOT_SENT").formatted(firstThread.getAsMention()), t);
                createLink(firstThread, t);
            });
        });
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.update(event.getMessage()));
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> {
                    mirrorE.getMessages().stream().filter(m -> !m.getMessageId().equals(event.getMessageIdLong())).forEach(MirroredMessage::delete);
                    mirroredMessages.remove(mirrorE);
                });
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.getMessages().forEach(m -> m.addReaction(event.getReaction())));
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.getMessages().forEach(m -> m.removeReaction(event.getReaction())));
    }

    private boolean noMirrors = false;
    public void loadLinks() {
        if(noMirrors) return;
        if(!Files.exists(MIRROR_STORAGE_PATH)) {
            try {
                Files.createFile(MIRROR_STORAGE_PATH);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            noMirrors = true;
            return;
        }
        try(BufferedReader stream = new BufferedReader(new FileReader(MIRROR_STORAGE_PATH.toFile()))) {
            String line;
            while((line = stream.readLine()) != null) {
                String[] s = line.split(";");
                TextChannel a = Main.getJDA().getTextChannelById(s[0]);
                TextChannel b = Main.getJDA().getTextChannelById(s[1]);
                if(a == null || b == null) return;
                if(existsLink(a, b)) return;
                mirrors.add(new Mirror(a, b));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeMirrors() {
        try(FileWriter stream = new FileWriter(MIRROR_STORAGE_PATH.toFile())) {
            mirrors.forEach(m -> {
                try {
                    stream.append(m.first.getId()).append(";").append(String.valueOf(m.second.getIdLong())).append("\n");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MirroredMessages {

        private final MirroredMessage initial;
        private final List<MirroredMessage> mirrored;
        private final List<Long> updatedIds = new ArrayList<>();

        private MirroredMessages(MirroredMessage initial, List<MirroredMessage> mirrored) {
            this.initial = initial;
            this.mirrored = mirrored;
        }

        public boolean match(Long messageId) {
            return initial.getMessageId().equals(messageId) || mirrored.stream().anyMatch(m -> m.getMessageId().equals(messageId));
        }

        public List<MirroredMessage> getMessages() {
            List<MirroredMessage> list = new ArrayList<>(mirrored);
            list.add(initial);
            return list;
        }

        public void update(Message message) {
            if(updatedIds.contains(message.getIdLong())) return;
            for (MirroredMessage mirroredMessage : getMessages()) {
                if(mirroredMessage.getMessageId() == message.getIdLong()) continue;
                updatedIds.add(mirroredMessage.getMessageId());
                mirroredMessage.update(message);
            }
            updatedIds.clear();
        }

    }

    record Mirror(@NotNull GuildMessageChannel first, @NotNull GuildMessageChannel second) {

        public GuildMessageChannel other(GuildMessageChannel channel) {
            return first.getIdLong()  == channel.getIdLong() ? second : first;
        }

        public boolean has(GuildMessageChannel channel) {
            return first.getIdLong() == channel.getIdLong() || second.getIdLong() == channel.getIdLong();
        }

    }

}