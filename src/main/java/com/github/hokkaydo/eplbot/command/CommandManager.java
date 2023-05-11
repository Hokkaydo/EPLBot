package com.github.hokkaydo.eplbot.command;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandManager extends ListenerAdapter {

    private final Map<Long, Command> commands = new HashMap<>();
    public void removeCommand(Long commandId) {
        if(!commands.containsKey(commandId)) return;
        Main.getJDA().deleteCommandById(commandId).queue(v -> commands.remove(commandId));
    }

    public void addGuildCommand(Long guildId, List<Command> commands, List<Long> addedCommandIds) {
        Guild guild = Main.getJDA().getGuildById(guildId);
        if(guild == null) return;
        List<SlashCommandData> data = new ArrayList<>(toCommandData(commands));
        List<net.dv8tion.jda.api.interactions.commands.Command> list = guild.retrieveCommands().complete();
        list.stream()
                .filter(command -> this.commands.values().stream().anyMatch(c -> c.getName().equals(command.getName())))
                .filter(command -> commands.stream().noneMatch(c -> c.getName().equals(command.getName())))
                .forEach(cmd -> data.add(SlashCommandData.fromCommand(cmd)));
        guild.updateCommands().addCommands(data).map(l -> mapWithIds(l, commands)).queue(ids -> {
                    this.commands.clear();
                    this.commands.putAll(ids);
                    addedCommandIds.addAll(ids.entrySet().stream().filter(e -> commands.stream().anyMatch(c -> e.getValue().getName().equals(c.getName()))).map(Map.Entry::getKey).toList());
                });
    }

    private Map<Long, Command> mapWithIds(List<net.dv8tion.jda.api.interactions.commands.Command> jdaCommands, List<Command> commands) {
        return jdaCommands.stream()
                .filter(c -> commands.stream().anyMatch(cmd -> cmd.getName().equals(c.getName())))
                .map(c -> Map.entry(c.getIdLong(), commands.stream().filter(cmd -> cmd.getName().equals(c.getName())).findFirst().get()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<SlashCommandData> toCommandData(List<Command> commands) {
        return commands.stream().map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription().get())
                .addOptions(cmd.getOptions())
                .setDefaultPermissions(cmd.adminOnly() ? DefaultMemberPermissions.DISABLED : DefaultMemberPermissions.ENABLED)).toList();
    }

    public List<Long> addGlobalCommand(List<Command> commands) {
        List<SlashCommandData> data = toCommandData(commands);
        Map<Long, Command> ids = Main.getJDA().retrieveCommands().flatMap(list -> {
                    list.forEach(cmd -> data.add(SlashCommandData.fromCommand(cmd)));
                    return Main.getJDA().updateCommands().addCommands(data).map(l -> mapWithIds(l, commands));
                })
                .complete();
        this.commands.putAll(ids);
        return Arrays.asList(ids.keySet().toArray(new Long[0]));
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(!commands.containsKey(event.getCommandIdLong())) return;
        Command command = commands.get(event.getCommandIdLong());
        if(!command.validateChannel(event.getMessageChannel())) {
            event.reply(Strings.getString("COMMAND_WRONG_CHANNEL")).setEphemeral(true).queue();
            return;
        }

        command.executeCommand(new CommandContext(event.getName(),
                event.getOptions(),
                event.getMember(),
                event.getMessageChannel(),
                event.getCommandType(),
                event.getInteraction(),
                event.getHook(),
                event.deferReply(command.ephemeralReply())
        ));
    }
}