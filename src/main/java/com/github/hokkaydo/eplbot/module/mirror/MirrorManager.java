package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.mirror.model.MirrorLink;
import com.github.hokkaydo.eplbot.module.mirror.repository.MirrorLinkRepository;
import com.github.hokkaydo.eplbot.module.mirror.repository.MirrorLinkRepositorySQLite;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MirrorManager extends ListenerAdapter {

    private final List<MirroredMessages> mirroredMessages = new ArrayList<>();
    private final MirrorLinkRepository mirrorLinkRepository;

    private final List<Long> threadWaitingFirstMirror = new ArrayList<>();

    public MirrorManager() {
        runPeriodicCleaner();
        this.mirrorLinkRepository = new MirrorLinkRepositorySQLite(DatabaseManager.getDataSource());
    }

    void createLink(GuildMessageChannel first, GuildMessageChannel second) {
        if(existsLink(first, second)) return;
        mirrorLinkRepository.create(new MirrorLink(first.getIdLong(), second.getIdLong()));
    }

    private void runPeriodicCleaner() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> mirroredMessages.removeIf(MirroredMessages::isOutdated), 0, 1, TimeUnit.HOURS);
    }

    List<MirrorLink> getLinks(GuildMessageChannel channel) {
        return mirrorLinkRepository.readyById(channel.getIdLong());
    }


    boolean existsLink(GuildMessageChannel first, GuildMessageChannel second) {
        return mirrorLinkRepository.exists(first.getIdLong(), second.getIdLong());
    }

    void destroyLink(GuildMessageChannel first, GuildMessageChannel second) {
        mirrorLinkRepository.deleteByIds(first.getIdLong(), second.getIdLong());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(threadWaitingFirstMirror.contains(event.getChannel().getIdLong())) {
            threadWaitingFirstMirror.remove(event.getChannel().getIdLong());
            sendMirror(event.getMessage());
            return;
        }

        if(event.getMessage().isEphemeral()) return;
        if(event.getMessage().getType().isSystem()) return;
        if(!event.getChannel().getType().isGuild()) return;
        if(event.isWebhookMessage() && event.getMessage().getType() != MessageType.SLASH_COMMAND) return;

        // do not mirror already mirrored messages
        if(mirroredMessages.stream().flatMap(m -> m.getMessages().entrySet().stream()).anyMatch(m -> m.getKey() == event.getMessageIdLong())) return;


        // If thread starter, create mirror thread
        if(event.getMessage().getType().equals(MessageType.THREAD_STARTER_MESSAGE) || event.getMessage().getType().equals(MessageType.THREAD_CREATED)) {
            ThreadChannel threadChannel = event.getMessage().getChannel().asThreadChannel();
            Message parentMessage = threadChannel.retrieveParentMessage().complete();
            mirroredMessages.stream()
                    .filter(m -> m.match(parentMessage.getIdLong()))
                    .flatMap(MirroredMessages::getMirrors)
                    .filter(m -> !m.isThreadOwner())
                    .forEach(mirroredMessage -> {
                        GuildChannel channel = Main.getJDA().getGuildChannelById(mirroredMessage.getChannelId());
                        if(!(channel instanceof GuildMessageChannel)) return;
                        createThread(
                                mirroredMessage.isMirror() ? mirroredMessage.getMirrorMessageId() : mirroredMessage.getOriginalMessageId(),
                                mirroredMessage.getChannelId(),
                                threadChannel
                        );
                        mirroredMessage.setThreadOwner();
                        threadWaitingFirstMirror.add(threadChannel.getIdLong());
                    });
            return;
        }
        sendMirror(event.getMessage());
    }

    /**
     * Mirror a given message
     * @param originalMessage the message to mirror
     * */
    private void sendMirror(Message originalMessage) {
        GuildMessageChannel originalChannel = originalMessage.getGuildChannel();
        boolean reply = originalMessage.getType().equals(MessageType.INLINE_REPLY) && originalMessage.getReferencedMessage() != null;
        originalChannel.getGuild().loadMembers().onSuccess(originalMembers -> {
            MirroredMessage initial = new MirroredMessage(originalMessage, originalChannel, originalMembers);
            MirroredMessages messages = new MirroredMessages(initial, new HashMap<>());
            List<MirrorLink> toDelete = new ArrayList<>();
            mirrorLinkRepository.readAll()
                    .stream()
                    .map(link -> {
                        if(link.first() == null || link.second() == null) {
                            toDelete.add(link);
                            return null;
                        }
                        return link;
                    })
                    .filter(Objects::nonNull)
                    .filter(link -> link.has(originalChannel))
                    .forEach(mirror -> {
                        GuildMessageChannel other = mirror.other(originalChannel);
                        other.getGuild().loadMembers().onSuccess(mirrorMembers -> {
                            MirroredMessage mirroredMessage = new MirroredMessage(originalMessage, other, mirrorMembers);
                            Consumer<Message> sentMessage = m -> messages.mirrored.put(m.getIdLong(), mirroredMessage);
                            if(reply) {
                                mirroredMessages.stream()
                                        // Get a stream of message's group related
                                        // (original or mirror ==) to the referenced message
                                        .filter(m -> m.match(originalMessage.getReferencedMessage().getIdLong()))
                                        .findFirst()
                                        // Get the only message of the group sent in the "other" channel
                                        .flatMap(message -> message.getMessages().entrySet().stream().filter(m -> m.getValue().getChannelId() == other.getIdLong()).findFirst())
                                        // Map to Tuple<<MessageId, MirroredMessage>, MirrorChannel>
                                        .map(m -> new Tuple2<>(m, Main.getJDA().getChannelById(GuildMessageChannel.class, m.getValue().getChannelId())))
                                        // Map to "replyMessageMirror"
                                        .map(t -> t.b.retrieveMessageById(t.a.getKey()))
                                        .ifPresentOrElse(
                                                a -> a.queue(replyMsg -> mirroredMessage.mirrorMessage(replyMsg, sentMessage)),
                                                () -> mirroredMessage.mirrorMessage(null, sentMessage)
                                        );
                            } else mirroredMessage.mirrorMessage(null, sentMessage);
                        });
                    });
            toDelete.forEach(mirrorLinkRepository::delete);
            mirroredMessages.add(messages);
        });
    }

    private record Tuple2<A, B>(A a, B b){}

    private void createThread(Long mirrorStarterMessageId, Long mirrorChannelId, ThreadChannel firstThread) {
        TextChannel channel = Main.getJDA().getChannelById(TextChannel.class, mirrorChannelId);
        if(channel == null) return;
        channel.retrieveMessageById(mirrorStarterMessageId).queue(m -> {
            if(m.getStartedThread() != null) return;
            m.createThreadChannel(firstThread.getName()).queue(t -> createLink(firstThread, t));
        });
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        mirrorLinkRepository.readyById(event.getChannel().getIdLong()).forEach(mirrorLinkRepository::delete);
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

    private static class MirroredMessages {

        // Original message (sent by real user)
        private final MirroredMessage initial;
        // Key: messageId of MirroredMessage, value: a mirror of "initial"
        private final Map<Long, MirroredMessage> mirrored;
        // Temp list used to avoid cycling update in "update" function
        private final List<Long> updatedIds = new ArrayList<>();
        // TTL to avoid too much memory consumption
        private final Instant outdatedTime;

        private MirroredMessages(MirroredMessage initial, Map<Long, MirroredMessage> mirrored) {
            this.initial = initial;
            this.mirrored = mirrored;
            this.outdatedTime = Instant.now().plus(24, ChronoUnit.HOURS);
        }

        /**
         * Check if given messageId is associated to this group
         * @param messageId message's id to check belonging
         * @return true if messageId is initial' id or an initial's mirror's id
         * */
        boolean match(Long messageId) {
            return initial.getOriginalMessageId().equals(messageId) || mirrored.containsKey(messageId);
        }

        /**
         * Update all mirrored messages based on given one
         * @param message the message to update on
         * */
        void update(Message message) {
            if(updatedIds.contains(message.getIdLong())) return;
            for (Map.Entry<Long, MirroredMessage> mirroredMessage : getMessages().entrySet()) {
                if(mirroredMessage.getKey() == message.getIdLong()) continue;
                if(mirroredMessage.getValue().isMirror())
                    updatedIds.add(mirroredMessage.getValue().getMirrorMessageId());
                mirroredMessage.getValue().update(message);
            }
            updatedIds.clear();
        }

        /**
         * Get all messages
         * @return a {@link Map} containing messages ids mapped with messages (initial and mirrors)
         * */
        Map<Long, MirroredMessage> getMessages() {
            Map<Long, MirroredMessage> map = new HashMap<>(mirrored);
            map.put(initial.getOriginalMessageId(), initial);
            return map;
        }

        /**
         * Check if the TTL is outdated
         * @return true if the TTL is outdated, false otherwise
         * */
        boolean isOutdated() {
            return Instant.now().isAfter(outdatedTime);
        }

        /**
         * Get all mirrored messages of this group
         * @return a {@link Stream<MirroredMessage>} of mirrored messages
         * */
        public Stream<MirroredMessage> getMirrors() {
            return getMessages().values().stream();
        }

    }

}