package com.github.hokkaydo.eplbot.module.configuration;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ConfigurationCommand implements Command {

    private final Long guildId;
    public ConfigurationCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> keyOption = context.options().stream().filter(s -> s.getName().equals("key")).findFirst();
        Optional<OptionMapping> valueOption = context.options().stream().filter(s -> s.getName().equals("value")).findFirst();
        if(keyOption.isEmpty()) {
            context.replyCallbackAction().setContent(
                    Config.DEFAULT_CONFIGURATION.keySet().stream()
                            .map(k ->  "`" + k + "`: " + Config.getGuildVariable(guildId, k))
                            .reduce((s1, s2) -> s1 + "\n" + s2)
                            .orElse("")
            ).queue();
            return;
        }
        if(valueOption.isEmpty()) {
            context.replyCallbackAction().setContent(
                    Config.DEFAULT_CONFIGURATION.keySet().stream()
                            .filter(k -> k.equals(keyOption.get().getAsString()))
                            .map(k -> "`" + k + "`: " + Config.getGuildVariable(guildId, k))
                            .findFirst()
                            .orElse("")
            ).queue();
            return;
        }
        boolean success = Config.parseAndUpdate(guildId, keyOption.get().getAsString(), valueOption.get().getAsString());
        if(success) {
            context.replyCallbackAction().setContent(String.format(Strings.getString("COMMAND_CONFIG_UPDATED"), keyOption.get().getAsString(), valueOption.get().getAsString())).queue();
        } else {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_CONFIG_UNKNOWN_VARIABLE")).queue();
        }
    }

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CONFIG_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Arrays.asList(
                new OptionData(OptionType.STRING, "key", Strings.getString("COMMAND_CONFIG_OPTION_KEY_DESCRIPTION"), false)
                        .addChoices(Config.DEFAULT_CONFIGURATION.keySet().stream().map(s -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(s, s)).toList()),
                new OptionData(OptionType.STRING, "value", Strings.getString("COMMAND_CONFIG_OPTION_VALUE_DESCRIPTION"), false)
        );
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
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_CONFIG_HELP").formatted(Config.DEFAULT_CONFIGURATION.keySet().stream().map(key -> "`" + key + "`: " + Config.getValueFormat(key)).reduce((s1, s2) -> s1+"\n\t"+s2).orElse(""));
    }

}
