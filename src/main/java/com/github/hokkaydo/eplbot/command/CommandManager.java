package com.github.hokkaydo.eplbot.command;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager extends ListenerAdapter {

    private final Map<Long, Map<Class<? extends Command>, Boolean>> commandStatus = new HashMap<>();
    private final Map<Long, Map<String, Command>> commands = new HashMap<>();
    private final Map<String, Command> globalCommands = new HashMap<>();
    private final Map<Class<? extends Command>, Boolean> globalCommandStatus = new HashMap<>();

    public void disableCommands(Long guildId, List<Class<? extends Command>> commands) {
        if(!this.commandStatus.containsKey(guildId)) return;
        Map<Class<? extends Command>, Boolean> status = this.commandStatus.get(guildId);
        for (Class<? extends Command> command : commands) {
            status.put(command, false);
        }
        this.commandStatus.put(guildId, status);
    }

    public void enableCommands(Long guildId, List<Class<? extends Command>> commands) {
        Map<Class<? extends Command>, Boolean> status = this.commandStatus.getOrDefault(guildId, new HashMap<>());
        for (Class<? extends Command> command : commands) {
            status.put(command, true);
        }
        this.commandStatus.put(guildId, status);
    }

    @SuppressWarnings("unused")
    public void disableGlobalCommands(List<Class<? extends Command>> commands) {
        for (Class<? extends Command> command : commands) {
            this.globalCommandStatus.put(command, false);
        }
    }

    public void enableGlobalCommands(List<Class<? extends Command>> commands) {
        for (Class<? extends Command> command : commands) {
            this.globalCommandStatus.put(command, true);
        }
    }

    public void addCommands(Guild guild, List<Command> commands) {
        Map<String, Command> guildCommands = this.commands.getOrDefault(guild.getIdLong(), new HashMap<>());
        for (Command command : commands) {
            guildCommands.put(command.getName(), command);
        }
        guild.retrieveCommands().queue(s -> {
            if(s.size() == commands.size()) return;
            guild.updateCommands().addCommands(guildCommands.values().stream().map(this::mapToCommandData).toList()).queue();
        });
        this.commands.put(guild.getIdLong(), guildCommands);
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Command command;
        if(!event.isGuildCommand() || event.getGuild() == null) {
            command = globalCommands.get(event.getFullCommandName());
            if(command == null) {
                event.reply(Strings.getString("COMMAND_NOT_FOUND")).setEphemeral(true).queue();
                return;
            }
            if(Boolean.FALSE.equals(globalCommandStatus.getOrDefault(command.getClass(), false))) {
                event.reply(Strings.getString("COMMAND_DISABLED")).setEphemeral(true).queue();
                return;
            }
        }else {
            if(event.getGuild() == null) return;
            command = commands.get(event.getGuild().getIdLong()).get(event.getFullCommandName());
            if(command == null) {
                event.reply(Strings.getString("COMMAND_NOT_FOUND")).setEphemeral(true).queue();
                return;
            }
            if(Boolean.FALSE.equals(commandStatus.get(event.getGuild().getIdLong()).getOrDefault(command.getClass(), false))) {
                event.reply(Strings.getString("COMMAND_DISABLED")).setEphemeral(true).queue();
                return;
            }
        }

        if(!command.validateChannel(event.getMessageChannel())) {
            event.reply(Strings.getString("COMMAND_WRONG_CHANNEL")).setEphemeral(true).queue();
            return;
        }

        command.executeCommand(new CommandContext(event.getName(),
                event.getOptions(),
                event.getUser(),
                event.getMember(),
                event.getMessageChannel(),
                event.getCommandType(),
                event.getInteraction(),
                event.getHook(),
                event.deferReply(command.ephemeralReply())
        ));
    }

    public List<Command> getCommands(Long guildId) {
        return new ArrayList<>(commands.getOrDefault(guildId, new HashMap<>()).values());
    }
    public boolean isEnabled(Long guild, Class<? extends Command> commandClazz) {
        return commandStatus.getOrDefault(guild, new HashMap<>()).getOrDefault(commandClazz, false);
    }

    public void addGlobalCommands(List<Command> commands) {
        Map<String, Command> newGlobals = new HashMap<>(this.globalCommands);
        for (Command command : commands) {
            newGlobals.put(command.getName(), command);
        }
        Main.getJDA().retrieveCommands().queue(s -> {
            if(s.size() == commands.size()) return;
            Main.getJDA().updateCommands().addCommands(newGlobals.values().stream().map(this::mapToCommandData).toList()).queue();
        });
        this.globalCommands.clear();
        this.globalCommands.putAll(newGlobals);
    }

    private CommandData mapToCommandData(Command cmd) {
        return Commands.slash(cmd.getName(), cmd.getDescription().get())
                .addOptions(cmd.getOptions())
                .setDefaultPermissions(cmd.adminOnly() ? DefaultMemberPermissions.DISABLED : DefaultMemberPermissions.ENABLED);
    }

    public void refreshCommands(Guild guild) {
        guild.updateCommands().addCommands(commands.get(guild.getIdLong()).values().stream().map(this::mapToCommandData).toList()).queue();
    }

}