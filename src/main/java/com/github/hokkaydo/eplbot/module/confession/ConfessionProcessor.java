package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
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

    private static final Map<UUID, MessageCreateBuilder> confessions = new HashMap<>();
    private static final List<UUID> confessFollowing = new ArrayList<>();
    private static final Map<UUID, Long> confessionAuthor = new HashMap<>();
    private static final String[] VALIDATION_EMBED_TITLES = {"Confession - Validée", "Confession - Refusée", "Confession - Signalée"};
    private static final Color[] VALIDATION_EMBED_COLORS = {Color.GREEN, Color.RED, Color.YELLOW};
    private static final int VALID = 0;
    private static final int REFUSED = 1;
    private static final int WARNED = 2;

    private static final String CONFESSION = "confession";

    private final Long guildId;
    private final Map<Long, Long> lastMainConfession;
    private final Map<Long, Long> lastMainConfessionValidation;
    private final Map<Long, Long> lastFollowingConfessionValidation;
    public ConfessionProcessor(Long guildId, Map<Long, Long> lastMainConfession) {
        this.guildId = guildId;
        this.lastMainConfession = lastMainConfession;
        this.lastFollowingConfessionValidation = new HashMap<>();
        this.lastMainConfessionValidation = new HashMap<>();
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
        lastFollowingConfessionValidation.remove(confessionAuthor.get(uuid));
        confessionChannel.sendMessage(confession.build()).queue(m -> lastMainConfession.put(confessionAuthor.get(uuid), m.getIdLong()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        assert id != null;
        if(id.contains(CONFESSION)) {
            UUID uuid = UUID.fromString(id.split(";")[1]);
            if(id.startsWith("validate")) {
                sendConfession(uuid, event.getGuild().getIdLong());
                updateValidationEmbedColor(VALID, event.getMessage());
            } else if(id.startsWith("refuse")){
                updateValidationEmbedColor(REFUSED, event.getMessage());
            } else {
                updateValidationEmbedColor(WARNED, event.getMessage());
                Main.getJDA().retrieveUserById(confessionAuthor.get(uuid)).flatMap(User::openPrivateChannel).queue(privateChannel -> privateChannel.sendMessage(Strings.getString("COMMAND_CONFESSION_WARN_AUTHOR")).queue());
            }
            event.getMessage().editMessageComponents(Collections.emptyList()).queue();
            return;
        }
        event.reply("unknown").queue();
    }
    private void updateValidationEmbedColor(int state, Message message) {
        if(message.getEmbeds().isEmpty() || message.getEmbeds().get(0).getFields().isEmpty()) return;
        MessageEmbed embed = message.getEmbeds().get(0);
        EmbedBuilder builder = new EmbedBuilder(embed).setColor(VALIDATION_EMBED_COLORS[state]).clearFields().addField(VALIDATION_EMBED_TITLES[state], Objects.requireNonNull(embed.getFields().get(0).getValue()), true);
        message.editMessageEmbeds(builder.build()).queue();
    }

}
