package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ConfessionCommand extends ListenerAdapter implements Command {

    private static final String CONFESSION = "confession";

    private static final Map<UUID, MessageCreateBuilder> pendingConfessions = new HashMap<>();
    private static final String[] VALIDATION_EMBED_TITLES = {"Confession - Validée", "Confession - Refusée"};
    private static final Color[] VALIDATION_EMBED_COLORS = {Color.GREEN, Color.RED};
    private static final int VALID = 0;
    private static final int REFUSED = 1;

    private final Long guildId;
    public ConfessionCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
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
        pendingConfessions.put(confessUUID, embedBuilder);

        validationChannel.sendMessage(MessageCreateBuilder.from(embedBuilder.build())
                                              .addActionRow(
                                                      Button.primary("validate-confession-" + confessUUID, Emoji.fromUnicode("✅")),
                                                      Button.primary("refuse-confession-" + confessUUID, Emoji.fromUnicode("❌"))
                                              ).build()).queue();
    }

    private void validateConfession(UUID uuid, Long guildId) {
        MessageCreateBuilder confession = pendingConfessions.get(uuid);
        if(confession == null) return;
        TextChannel confessionChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId,"CONFESSION_CHANNEL_ID"));
        if(confessionChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("WARNING_CONFESSION_CHANNEL_ID_INVALID"), guildId);
            return;
        }
        confessionChannel.sendMessage(confession.build()).queue();
        pendingConfessions.remove(uuid);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if(event.getGuild() == null) return;
        assert id != null;
        if(id.contains(CONFESSION)) {
            if(id.startsWith("validate")) {
                validateConfession(UUID.fromString(id.replace("validate-confession-", "")), event.getGuild().getIdLong());
                updateEmbedColor(VALID, event.getMessage());
            } else {
                updateEmbedColor(REFUSED, event.getMessage());
            }
            event.getMessage().editMessageComponents(Collections.emptyList()).queue();
            return;
        }
        event.reply("unknown").queue();
    }
    private void updateEmbedColor(int state, Message message) {
        if(message.getEmbeds().isEmpty() || message.getEmbeds().get(0).getFields().isEmpty()) return;
        MessageEmbed embed = message.getEmbeds().get(0);
        EmbedBuilder builder = new EmbedBuilder(embed).setColor(VALIDATION_EMBED_COLORS[state]).clearFields().addField(VALIDATION_EMBED_TITLES[state], Objects.requireNonNull(embed.getFields().get(0).getValue()), true);
        message.editMessageEmbeds(builder.build()).queue();
    }

    @Override
    public String getName() {
        return "confess";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CONFESSION_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, CONFESSION, Strings.getString("COMMAND_CONFESSION_OPTION_DESCRIPTION"), true));
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_CONFESSION_HELP");
    }

}
