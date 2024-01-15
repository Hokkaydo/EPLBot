package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EarlyBirdNextMessageCommand extends ListenerAdapter implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        if(!context.interaction().isGuildCommand() || context.interaction().getGuild() == null) return;
        long guildId = context.interaction().getGuild().getIdLong();
        String earlyBirdRoleId = Config.getGuildVariable(guildId, "EARLY_BIRD_ROLE_ID");
        if(context.author().getRoles().stream().filter(r -> r.getId().equals(earlyBirdRoleId)).findFirst().isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("EARLY_BIRD_NOT_EARLY_BIRD")).queue();
            return;
        }
        Modal modal = Modal.create("earlybird-message", "Message matinal")
                              .addActionRow(TextInput.create("message", "Message", TextInputStyle.PARAGRAPH).build())
                              .build();
        context.interaction().replyModal(modal).queue();
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "earlybirdmessage";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("EARLY_BIRD_NEXT_MESSAGE_COMMAND_DESCRIPTION");
    }

    @Override
    public void onModalInteraction (ModalInteractionEvent event) {
        if(event.getGuild() == null) return;
        String id = event.getModalId();
        if(!id.equals("earlybird-message")) return;
        ModalMapping contentMap = event.getInteraction().getValue("message");
        if(contentMap == null) return;
        String content = contentMap.getAsString();
        if(content.length() > Message.MAX_CONTENT_LENGTH) {
            event.reply(Strings.getString("EARLY_BIRD_MESSAGE_TOO_LONG").formatted(Message.MAX_CONTENT_LENGTH)).queue();
            return;
        }
        Config.updateValue(event.getGuild().getIdLong(), "EARLY_BIRD_NEXT_MESSAGE", content);
        MessageUtil.sendAdminMessage("Prochain message matinal enregistré par %s :%n >>> %s".formatted(event.getUser().getAsMention(), content), event.getGuild().getIdLong());
        event.reply(Strings.getString("EARLY_BIRD_NEXT_MESSAGE_REGISTERED")).queue();
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return channel instanceof GuildMessageChannel;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("EARLY_BIRD_NEXT_MESSAGE_COMMAND_HELP");
    }

}
