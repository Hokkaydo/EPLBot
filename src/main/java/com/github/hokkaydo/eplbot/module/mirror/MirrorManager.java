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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MirrorManager extends ListenerAdapter {

    public static final Path MIRROR_STORAGE_PATH = Path.of(Main.PERSISTENCE_DIR_PATH + "/mirrors");
    private final List<Mirror> mirrors = new ArrayList<>();
    private final List<MirroredMessages> mirroredMessages = new ArrayList<>();

    public void createLink(GuildMessageChannel first, GuildMessageChannel second) {
        if(existsLink(first, second)) return;
        mirrors.add(new Mirror(first, second));
        storeMirrors();
        runPeriodicCleaner();
    }

    private void runPeriodicCleaner() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> mirroredMessages.removeIf(MirroredMessages::isOutdated), 0, 1, TimeUnit.HOURS);
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
        if(mirroredMessages.stream().flatMap(m -> m.getMessages().entrySet().stream()).anyMatch(m -> m.getKey() == 0 || m.getKey() == event.getMessageIdLong())) return;
        if(event.getMessage().getType().equals(MessageType.THREAD_STARTER_MESSAGE) || event.getMessage().getType().equals(MessageType.THREAD_CREATED)) {
            ThreadChannel threadChannel = event.getMessage().getChannel().asThreadChannel();
            threadChannel.retrieveParentMessage().queue(parent -> mirrors.stream().filter(m -> m.has(event.getChannel().asThreadChannel().getParentMessageChannel())).forEach(mirror -> {
                GuildMessageChannel other = mirror.other(parent.getChannel().asGuildMessageChannel());
                mirroredMessages.stream()
                        .filter(m -> m.getMessages().values().stream().anyMatch(msg -> (msg.getMessageId() == parent.getIdLong()) && msg.getChannelId() == other.getIdLong()))
                        .flatMap(m -> m.getMessages().values().stream().filter(msg -> msg.getChannelId() == other.getIdLong()))
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
        MirroredMessage initial = new MirroredMessage(event.getMessage(), originalChannel);
        MirroredMessages messages = new MirroredMessages(initial, new HashMap<>());
        mirrors.stream().filter(m -> m.has(originalChannel)).forEach(mirror -> {
            GuildMessageChannel other = mirror.other(originalChannel);
            if(reply) {
                mirroredMessages.stream()
                        .filter(m -> m.match(event.getMessage().getReferencedMessage().getIdLong()))
                        .findFirst()
                        .flatMap(message -> message.getMessages().entrySet().stream().filter(m -> m.getValue().getChannelId() == other.getIdLong()).findFirst())
                        .map(m -> new Tuple2<>(m, Main.getJDA().getChannelById(GuildMessageChannel.class, m.getValue().getChannelId())))
                        .map(t -> new Tuple2<>(t.a, t.b.retrieveMessageById(t.a.getKey())))
                        .map(Tuple2::b)
                        .ifPresentOrElse(a -> a.queue(replyMsg -> {
                            MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other);
                            mirroredMessage.mirrorMessage(replyMsg, m -> messages.mirrored.put(m.getIdLong(), mirroredMessage));
                        }), () -> {
                            MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other);
                            mirroredMessage.mirrorMessage(null, m -> messages.mirrored.put(m.getIdLong(), mirroredMessage));
                        });
            }
            else {
                MirroredMessage mirroredMessage = new MirroredMessage(event.getMessage(), other);
                mirroredMessage.mirrorMessage(null, m -> messages.mirrored.put(m.getIdLong(), mirroredMessage));
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
                    mirrorE.getMessages().entrySet().stream().filter(m -> !m.getKey().equals(event.getMessageIdLong())).map(Map.Entry::getValue).forEach(MirroredMessage::delete);
                    mirroredMessages.remove(mirrorE);
                });
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Main.getJDA().getUserById(347348560389603329L).openPrivateChannel().queue(channel -> event.retrieveMessage().queue(m -> {
            channel.sendMessage("%s%n%s".formatted( m.getContentRaw(), event.getReaction().toString())).queue();
        }));
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.getMessages().forEach((id, m) -> m.addReaction(event.getReaction())));
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.match(event.getMessageIdLong()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.getMessages().forEach((id, m) -> m.removeReaction(event.getReaction())));
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
        private final Map<Long, MirroredMessage> mirrored;
        private final List<Long> updatedIds = new ArrayList<>();
        private final Instant outdatedTime;

        private MirroredMessages(MirroredMessage initial, Map<Long, MirroredMessage> mirrored) {
            this.initial = initial;
            this.mirrored = mirrored;
            this.outdatedTime = Instant.now().plus(24, ChronoUnit.HOURS);
        }

        public boolean match(Long messageId) {
            return initial.getMessageId().equals(messageId) || mirrored.containsKey(messageId);
        }

        public Map<Long, MirroredMessage> getMessages() {
            Map<Long, MirroredMessage> map = new HashMap<>(mirrored);
            map.put(initial.getMessageId(), initial);
            return map;
        }

        public void update(Message message) {
            if(updatedIds.contains(message.getIdLong())) return;
            for (Map.Entry<Long, MirroredMessage> mirroredMessage : getMessages().entrySet()) {
                if(mirroredMessage.getKey() == message.getIdLong()) continue;
                updatedIds.add(mirroredMessage.getValue().getMessageId());
                mirroredMessage.getValue().update(message);
            }
            updatedIds.clear();
        }

        public boolean isOutdated() {
            return Instant.now().isAfter(outdatedTime);
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