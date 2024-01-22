package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class HelpCommand implements Command {

    private final Long guildId;
    public HelpCommand(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void executeCommand(CommandContext context) {
        List<String> helps = Main.getCommandManager().getCommands(guildId).stream()
                                     .filter(c -> Main.getCommandManager().isEnabled(guildId, c.getClass()) || context.author().hasPermission(Permission.ADMINISTRATOR))
                                     .map(c -> STR."__\{c.getName()}__: \n\{c.help().get()}")
                                     .toList();
        StringBuilder stringBuilder = new StringBuilder("__AIDE :__");
        boolean firstSent = false;
        for (String help : helps) {
            StringBuilder temp = new StringBuilder(stringBuilder);
            temp.append("\n\n").append(help);
            if (temp.length() > 2000) {
                if (firstSent) {
                    context.hook().sendMessage(stringBuilder.toString()).queue();
                } else {
                    context.replyCallbackAction().setContent(stringBuilder.toString()).queue();
                    firstSent = true;
                }
                stringBuilder = new StringBuilder();
                continue;
            }
            stringBuilder = temp;
        }
        if(stringBuilder.isEmpty()) return;
        if (firstSent) {
            context.hook().sendMessage(stringBuilder.toString()).queue();
            return;
        }
        context.replyCallbackAction().setContent(stringBuilder.toString()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_HELP_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
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
        return () -> Strings.getString("COMMAND_HELP_HELP");
    }

}
