package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class StateCommand implements Command {

    private final Long guildId;
    private static final String RESET = "reset";
    public StateCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> subCommand = context.options().stream().filter(s -> s.getName().equals("subcommand")).findFirst();
        if(subCommand.isEmpty()) {
            context.replyCallbackAction().setContent(
                    Config.getDefaultState().keySet().stream()
                            .map(k ->  "`" + k + "`: " + Config.getGuildState(guildId, k))
                            .reduce((s1, s2) -> s1 + "\n" + s2)
                            .orElse("")
            ).queue();
            return;
        }
        if(subCommand.get().getAsString().equalsIgnoreCase(RESET)) {
            Config.resetDefaultState(guildId);
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_STATE_RESET")).queue();
            return;
        }
        context.replyCallbackAction().setContent(Strings.getString("COMMAND_STATE_ACTION_NOT_FOUND").formatted(subCommand.get().getAsString())).queue();
    }

    @Override
    public String getName() {
        return "state";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_STATE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "subcommand", Strings.getString("COMMAND_STATE_SUBCOMMAND_OPTION_DESCRIPTION"), false)
                               .addChoice(RESET, RESET));
    }

    @Override
    public boolean ephemeralReply() {
        return false;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return !(channel instanceof PrivateChannel);
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_STATE_HELP");
    }

}
