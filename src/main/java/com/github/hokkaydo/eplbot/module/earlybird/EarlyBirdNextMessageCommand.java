package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class EarlyBirdNextMessageCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Optional<String> messageOpt = context.options().stream().filter(o -> o.getName().equals("message")).findFirst().map(OptionMapping::getAsString);
        if(messageOpt.isEmpty()) return;
        if(!context.interaction().isGuildCommand() || context.interaction().getGuild() == null) return;
        long guildId = context.interaction().getGuild().getIdLong();
        String earlyBirdRoleId = Config.getGuildVariable(guildId, "EARLY_BIRD_ROLE_ID");
        if(context.author().getRoles().stream().filter(r -> r.getId().equals(earlyBirdRoleId)).findFirst().isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("EARLY_BIRD_NOT_EARLY_BIRD")).queue();
            return;
        }
        Config.updateValue(guildId, "EARLY_BIRD_NEXT_MESSAGE", messageOpt.get());
        MessageUtil.sendAdminMessage("Prochain message matinal enregistré par %s :%n >>> %s".formatted(context.author().getAsMention(), messageOpt.get()), guildId);
        context.replyCallbackAction().setContent(Strings.getString("EARLY_BIRD_NEXT_MESSAGE_REGISTERED")).queue();
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
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "message", "Prochain message matinal que le bot enverra"));
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
