package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DebugCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        long bossId = 347348560389603329L;
        if(context.author().getIdLong() != bossId) {
            context.replyCallbackAction().setContent("You're not allowed to execute this commande (only my boss can use it)").queue();
            return;
        }
        List<String> filesNames = List.of(Config.CONFIG_PATH);
        context.author().getUser().openPrivateChannel().queue(channel -> {
            for (String filesName : filesNames) {
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    readFile(filesName).forEach(s -> stringBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    channel.sendMessage("Error while reading %s".formatted(filesName)).queue();
                    continue;
                }
                channel.sendMessage("---------------- %s ----------------%n%s".formatted(filesName, stringBuilder.toString())).queue();
            }
        });
    }

    private List<String> readFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        List<String> lines = new ArrayList<>();
        String s;
        while((s=reader.readLine()) != null) {
            lines.add(s);
        }
        return lines;
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> "Debug ONLY !";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean ephemeralReply() {
        return false;
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
        return () -> "DEBUG ONLY";
    }

}
