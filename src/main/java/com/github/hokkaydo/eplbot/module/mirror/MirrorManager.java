package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class MirrorManager extends ListenerAdapter {

    private static final Path MIRROR_STORAGE_PATH = Path.of("mirrors");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*$");
    private final List<Mirror> mirrors = new ArrayList<>();
    private final List<MirroredMessages> mirroredMessages = new ArrayList<>();

    public void createLink(MessageChannel first, MessageChannel second) {
        if(existsLink(first, second)) return;
        mirrors.add(new Mirror(first, second));
        storeMirrors();
    }

    public List<Mirror> getLinks(MessageChannel idLong) {
        return mirrors.stream().filter(m -> m.has(idLong)).toList();
    }

    public Optional<Mirror> getLink(MessageChannel first, MessageChannel second) {
        return mirrors.stream()
                       .filter(mirror -> (mirror.first.equals(first) && mirror.second.equals(second)) ||
                                                 (mirror.second.equals(first) && mirror.first.equals(second)))
                       .findFirst();
    }

    public boolean existsLink(MessageChannel first, MessageChannel second) {
        return getLink(first, second).isPresent();
    }

    public void destroyLink(MessageChannel first, MessageChannel second) {
        getLink(first, second).ifPresent(mirrors::remove);
        storeMirrors();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getMessage().isEphemeral()) return;
        if(event.getMessage().getType().isSystem()) return;
        AtomicReference<MessageCreateAction> action = new AtomicReference<>();
        mirrors.stream().filter(m -> m.has(event.getChannel())).forEach(mirror -> {
            MessageChannel other = mirror.other(event.getChannel());
            if(action.get() != null) {
                action.get().queue();
                return;
            }
            if(mirroredMessages.stream().anyMatch(mirrored -> mirrored.isMirror(event.getMessage()) && other.getId().equals(mirrored.initial.getChannel().getId()))) return;
            if(URL_PATTERN.matcher(event.getMessage().getContentRaw()).find()) {
                action.set(other.sendMessage(MessageCreateData.fromMessage(event.getMessage())));
            } else {
                MessageEmbed embed = MessageUtil.toEmbed(event.getMessage()).build();
                action.set(other.sendMessageEmbeds(embed));
            }
            event.getMessage().getAttachments().stream()
                    .map(m -> new Tuple3<>(m.getFileName(), m.getProxy().download(), m.isSpoiler()))
                    .map(tuple3 -> tuple3.b()
                                           .thenApply(i -> FileUpload.fromData(i, tuple3.a()))
                                           .thenApply(f -> Boolean.TRUE.equals(tuple3.c()) ? f.asSpoiler() : f)
                    )
                    .map(c -> c.thenAccept(f -> action.get().addFiles(f)))
                    .reduce((a,b) -> {a.join(); return b;})
                    .ifPresentOrElse(
                            c -> {c.join(); Message m = action.get().complete();
                                sentMessage(event.getMessage(), m);},
                            () -> {Message m = action.get().complete();
                                sentMessage(event.getMessage(), m);}
                    );
        });
    }

    private void sentMessage(Message initial, Message newMessage) {
        mirroredMessages.stream()
                .filter(m -> m.initial.getId().equals(initial.getId()))
                .findFirst()
                .ifPresentOrElse(
                        mirror -> mirror.mirrored.add(newMessage),
                        () -> mirroredMessages.add(new MirroredMessages(initial, new ArrayList<>(Collections.singletonList(newMessage))))
                );
    }
    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.initial.getId().equals(event.getMessageId()))
                .findFirst()
                .ifPresent(mirrorE -> mirrorE.mirrored.forEach(mirror -> {
                    mirror.editMessage(MessageEditData.fromEmbeds(MessageUtil.toEmbed(event.getMessage()).build())).queue();
                    if(event.getMessage().isPinned())
                        mirror.pin().queue();
                    else
                        mirror.unpin().queue();
                }));
    }
    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        mirroredMessages.stream()
                .filter(mirror -> mirror.initial.getId().equals(event.getMessageId()))
                .findFirst()
                .ifPresent(mirrorE -> {
                    mirrorE.mirrored.forEach(mirror -> mirror.delete().queue());
                    mirrorE.mirrored.clear();
                    mirroredMessages.remove(mirrorE);
                });
    }

    private boolean noMirrors = false;
    public void loadLinks() {
        if(noMirrors) return;
        if(!Files.exists(MIRROR_STORAGE_PATH)) {
            try {
                Files.createFile(MIRROR_STORAGE_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
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
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record MirroredMessages(Message initial, List<Message> mirrored) {

        public boolean isMirror(Message message) {
            return mirrored.stream().anyMatch(m -> m.getId().equals(message.getId()));
        }
    }

    record Mirror(@NotNull MessageChannel first, @NotNull MessageChannel second) {

        public MessageChannel other(MessageChannel channel) {
            return first.getIdLong()  == channel.getIdLong() ? second : first;
        }

        public boolean has(MessageChannel channel) {
            return first.getIdLong() == channel.getIdLong() || second.getIdLong() == channel.getIdLong();
        }

    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}

}