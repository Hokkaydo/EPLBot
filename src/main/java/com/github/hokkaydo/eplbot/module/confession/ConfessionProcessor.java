package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConfessionProcessor extends ListenerAdapter {

    private final Map<UUID, MessageCreateBuilder> confessions = new HashMap<>();
    private final Map<UUID, String> confessionsContent = new HashMap<>();
    private final List<UUID> confessFollowing = new ArrayList<>();
    private final Map<UUID, Long> confessionAuthor = new HashMap<>();
    private static final String[] VALIDATION_EMBED_TITLES = {"Confession - Validée", "Confession - Refusée", "Confession - Signalée"};
    private static final Color[] VALIDATION_EMBED_COLORS = {Color.GREEN, Color.RED, Color.YELLOW};
    private static final int VALID = 0;
    private static final int REFUSED = 1;
    private static final int WARNED = 2;
    private static final Path WARNED_CONFESSION_STORAGE_PATH = Path.of("%s/warned_confessions".formatted(Main.PERSISTENCE_DIR_PATH));

    private static final String CONFESSION = "confession";

    private final Long guildId;
    private final Map<Long, Long> lastMainConfession;
    private final Map<Long, Long> lastMainConfessionValidation;
    private final Map<Long, Long> lastFollowingConfessionValidation;
    private final List<WarnedConfessionRecord> warnedConfessions = new ArrayList<>();

    public ConfessionProcessor(Long guildId, Map<Long, Long> lastMainConfession) {
        this.guildId = guildId;
        this.lastMainConfession = lastMainConfession;
        this.lastFollowingConfessionValidation = new HashMap<>();
        this.lastMainConfessionValidation = new HashMap<>();
        loadWarnedConfessions();
    }

    public void process(CommandContext context, boolean following) {
        TextChannel validationChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId, "CONFESSION_VALIDATION_CHANNEL_ID"));
        if(validationChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("WARNING_CONFESSION_VALIDATION_CHANNEL_ID_INVALID"), guildId);
            context.replyCallbackAction().setContent(Strings.getString("ERROR_OCCURRED")).queue();
            return;
        }
        Optional<OptionMapping> confessionOption = context.options().stream().filter(o -> o.getName().equals(CONFESSION)).findFirst();
        if(confessionOption.isEmpty()) return;
        UUID confessUUID = UUID.randomUUID();
        context.replyCallbackAction().applyData(MessageCreateData.fromContent(Strings.getString("COMMAND_CONFESSION_SUBMITTED"))).queue();

        MessageCreateBuilder embedBuilder = MessageCreateBuilder.from(MessageCreateData.fromEmbeds(
                new EmbedBuilder()
                        .setColor(Config.getGuildVariable(guildId, "CONFESSION_EMBED_COLOR"))
                        .addField("Confession", confessionOption.get().getAsString(), true)
                        .setTimestamp(Instant.now())
                        .build()
        ));
        confessions.put(confessUUID, embedBuilder);
        confessionsContent.put(confessUUID, confessionOption.get().getAsString());
        confessionAuthor.put(confessUUID, context.user().getIdLong());
        MessageCreateBuilder data = MessageCreateBuilder.from(embedBuilder.build())
                                            .addActionRow(
                                                    Button.primary("validate-confession;" + confessUUID, Emoji.fromUnicode("✅")),
                                                    Button.primary("warn-confession;" + confessUUID, Emoji.fromUnicode("⚠")),
                                                    Button.primary("refuse-confession;" + confessUUID, Emoji.fromUnicode("❌"))
                                            );
        if(following) {
            confessFollowing.add(confessUUID);
            if(lastFollowingConfessionValidation.containsKey(context.user().getIdLong())) {
                validationChannel.retrieveMessageById(lastFollowingConfessionValidation.get(context.user().getIdLong())).queue(lastValidation -> lastValidation.reply(data.build()).queue());
                return;
            }
            validationChannel.retrieveMessageById(lastMainConfessionValidation.get(context.user().getIdLong())).queue(lastValidation -> lastValidation.reply(data.build()).queue(m -> lastFollowingConfessionValidation.put(context.user().getIdLong(), m.getIdLong())));
            return;
        }
        validationChannel.sendMessage(data.build()).queue(m -> lastMainConfessionValidation.put(context.user().getIdLong(), m.getIdLong()));
    }

    private void sendConfession(UUID uuid, Long guildId) {
        MessageCreateBuilder confession = confessions.get(uuid);
        confessions.remove(uuid);
        if(confession == null) return;
        TextChannel confessionChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId,"CONFESSION_CHANNEL_ID"));
        if(confessionChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("WARNING_CONFESSION_CHANNEL_ID_INVALID"), guildId);
            return;
        }
        if(confessFollowing.contains(uuid)) {
            confessionChannel.retrieveMessageById(lastMainConfession.get(confessionAuthor.get(uuid))).queue(m -> {
                ThreadChannel channel = m.getStartedThread();
                if(channel == null) {
                    m.createThreadChannel("Confession - Follow").queue(t -> {
                        if(t == null) {
                            MessageUtil.sendAdminMessage(Strings.getString("WARNING_CONFESSION_CHANNEL_ID_INVALID"), guildId);
                            return;
                        }
                        t.sendMessage(confession.build()).queue();
                    });
                    return;
                }
                channel.sendMessage(confession.build()).queue();
            });
            confessFollowing.remove(uuid);
            return;
        }
        Long authorId = confessionAuthor.get(uuid);
        confessionAuthor.remove(uuid);
        lastFollowingConfessionValidation.remove(authorId);
        confessionChannel.sendMessage(confession.build()).queue(m -> lastMainConfession.put(authorId, m.getIdLong()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        assert id != null;
        if(id.contains(CONFESSION)) {
            UUID uuid = UUID.fromString(id.split(";")[1]);
            if(id.startsWith("validate")) {
                updateValidationEmbedColor(VALID, event.getMessage());
                sendConfession(uuid, event.getGuild().getIdLong());
            } else if(id.startsWith("refuse")){
                updateValidationEmbedColor(REFUSED, event.getMessage());
            } else {
                updateValidationEmbedColor(WARNED, event.getMessage());
                warn(event.getUser().getIdLong(), uuid);
            }
            event.getMessage().editMessageComponents(Collections.emptyList()).queue();
            return;
        }
        event.reply("unknown").queue();
    }

    private void warn(Long moderatorId, UUID uuid) {
        Long confessionAuthorId = confessionAuthor.get(uuid);
        confessionAuthor.remove(uuid);
        WarnedConfessionRecord warnedConfessionRecord = new WarnedConfessionRecord(confessionsContent.get(uuid), moderatorId, confessionAuthorId, Timestamp.from(Instant.now()));
        warnedConfessions.add(warnedConfessionRecord);
        confessionsContent.remove(uuid);
        storeWarnedConfessions();
        int threshold = Config.<Integer>getGuildVariable(guildId, "CONFESSION_WARN_THRESHOLD");
        Guild guild = Main.getJDA().getGuildById(guildId);
        if(guild == null) return;
        List<WarnedConfessionRecord> authorWarnedConfessions = warnedConfessions.stream().filter(r -> Objects.equals(r.authorId, confessionAuthorId)).toList();
        Main.getJDA().retrieveUserById(confessionAuthorId).flatMap(User::openPrivateChannel).queue(privateChannel -> privateChannel.sendMessage(Strings.getString("COMMAND_CONFESSION_WARN_AUTHOR")).queue());

        if(authorWarnedConfessions.size() < threshold) return;
        TextChannel validationChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId,"CONFESSION_VALIDATION_CHANNEL_ID"));
        if(validationChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("WARNING_CONFESSION_VALIDATION_CHANNEL_ID_INVALID"), guildId);
            return;
        }
        Member user = guild.getMemberById(confessionAuthorId);
        EmbedBuilder builder = new EmbedBuilder()
                                       .setDescription(Strings.getString("CONFESSION_TOO_MUCH_WARNS")
                                                               .formatted(
                                                                       user == null ? "USER NOT ON SERVER" : user.getAsMention(),
                                                                       threshold,
                                                                       threshold,
                                                                       user == null ? "USER NOT ON SERVER" : "@" + user.getNickname()
                                                               )
                                       )
                                       .setColor(Color.ORANGE);
        validationChannel.sendMessageEmbeds(builder.build()).queue(m -> {
            for (WarnedConfessionRecord confession : authorWarnedConfessions) {
                Member mod = guild.getMemberById(confession.moderatorId);
                EmbedBuilder warned = new EmbedBuilder()
                                              .setFooter(Strings.getString("CONFESSION_WARNED_BY").formatted(mod == null ? confession.moderatorId : mod.getUser().getAsTag()))
                                              .setDescription(confession.content)
                                              .setColor(Color.BLACK)
                                              .setTimestamp(confession.timestamp.toInstant())
                                              .setAuthor(user == null ? String.valueOf(confessionAuthorId) : user.getUser().getAsTag());
                m.replyEmbeds(warned.build()).queue();
            }
        });
    }
    private void updateValidationEmbedColor(int state, Message message) {
        if(message.getEmbeds().isEmpty() || message.getEmbeds().get(0).getFields().isEmpty()) return;
        MessageEmbed embed = message.getEmbeds().get(0);
        EmbedBuilder builder = new EmbedBuilder(embed).setColor(VALIDATION_EMBED_COLORS[state]).clearFields().addField(VALIDATION_EMBED_TITLES[state], Objects.requireNonNull(embed.getFields().get(0).getValue()), true);
        message.editMessageEmbeds(builder.build()).queue();
    }

    public void loadWarnedConfessions() {
        if(!Files.exists(WARNED_CONFESSION_STORAGE_PATH)) {
            try {
                Files.createFile(WARNED_CONFESSION_STORAGE_PATH);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try(BufferedReader stream = new BufferedReader(new FileReader(WARNED_CONFESSION_STORAGE_PATH.toFile()))) {
            String line;
            while((line = stream.readLine()) != null) {
                if(line.isBlank()) continue;
                warnedConfessions.add(WarnedConfessionRecord.of(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeWarnedConfessions() {
        try(FileWriter stream = new FileWriter(WARNED_CONFESSION_STORAGE_PATH.toFile())) {
            for (WarnedConfessionRecord warnedConfession : warnedConfessions) {
                stream.append(warnedConfessions.size() == 1 ? "" : "\n").append(warnedConfession.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearWarnings(long idLong) {
        List<WarnedConfessionRecord> toDelete = warnedConfessions.stream().filter(r -> r.authorId == idLong).toList();
        toDelete.forEach(warnedConfessions::remove);
        storeWarnedConfessions();
    }

    private record WarnedConfessionRecord(String content, Long moderatorId, Long authorId, Timestamp timestamp) {
        private static WarnedConfessionRecord of(String line) {
            String[] split = line.split(";;;");
            return new WarnedConfessionRecord(split[0], Long.valueOf(split[1]), Long.valueOf(split[2]), Timestamp.valueOf(split[3]));
        }

        @Override
        public String toString() {
            return "%s;;;%s;;;%s;;;%s".formatted(content.replace(";;;", ";"), moderatorId.toString(), authorId.toString(), timestamp.toString());
        }

    }

}
