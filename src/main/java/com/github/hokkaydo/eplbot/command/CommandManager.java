package com.github.hokkaydo.eplbot.command;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.Permission;
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
import java.util.Objects;

/**
 * This class handles command registration and execution.
 * */
public class CommandManager extends ListenerAdapter {

    private final Map<Long, Map<String, Command>> commands = new HashMap<>();
    private final Map<String, Command> globalCommands = new HashMap<>();

    /**
     * Disables the given commands for the given guild.
     * @param guildId the id of the guild
     * @param commands a {@link List<Command>} of {@link Command} to disable
     */
    public void disableCommands(Long guildId, List<Command> commands) {
        Guild guild = Main.getJDA().getGuildById(guildId);
        if(guild == null) return;
        List<String> names = commands.stream().map(Command::getName).toList();
        guild.retrieveCommands()
                .queue(list -> list.stream()
                                       .filter(command -> names.stream().anyMatch(name -> Objects.equals(name, command.getName())))
                                       .forEach(command -> command.delete().queue())
                );
    }

    /**
     * Enables the given commands for the given guild.
     * @param guildId the id of the guild
     * @param commands a {@link List<Command>} of {@link Command} to enable
     */
    public void enableCommands(Long guildId, List<Command> commands) {
        Guild guild = Main.getJDA().getGuildById(guildId);
        if(guild == null) return;
        commands.stream().map(this::mapToCommandData).forEach(commandData -> guild.upsertCommand(commandData).queue());
    }

    /**
     * Maps a {@link Command} to a {@link CommandData} object.
     * Used in {@link #onSlashCommandInteraction(SlashCommandInteractionEvent)}
     * to pass the command to the command executor.
     * @param cmd the command to map
     * */
    private CommandData mapToCommandData(Command cmd) {
        return Commands.slash(cmd.getName(), cmd.getDescription().get())
                       .addOptions(cmd.getOptions())
                       .setDefaultPermissions(cmd.adminOnly() ? DefaultMemberPermissions.DISABLED : DefaultMemberPermissions.ENABLED);
    }

    /**
     * Adds the given commands to the given guildId.
     * @param commands a {@link List<Command>} of {@link Command} to enable
     */
    public void addCommands(Long guildId, List<Command> commands) {
        Map<String, Command> guildCommands = this.commands.getOrDefault(guildId, new HashMap<>());
        for (Command command : commands) {
            guildCommands.put(command.getName(), command);
        }
        this.commands.put(guildId, guildCommands);
    }

    /**
     * Command handler for slash commands.
     * This method is called whenever a slash command is executed.
     * It checks
     <ul>
     <li>if the command exists in the system</li>
     <li>if the owning module is enabled</li>
     <li>if the command is in the right channel</li>
     <li>if the user has the permission to execute the command</li>
     </ul>
     * Then it forwards the execution to the command executor.
     * @param event the {@link SlashCommandInteractionEvent} to handle
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Command command;
        if(!event.isGuildCommand() || event.getGuild() == null) {
            command = globalCommands.get(event.getFullCommandName());
        }else {
            command = commands.getOrDefault(event.getGuild().getIdLong(), new HashMap<>()).getOrDefault(event.getFullCommandName(), null);
        }
        if(command == null) {
            event.reply(Strings.getString("command.not_found")).setEphemeral(true).queue();
            return;
        }

        if(!command.validateChannel(event.getMessageChannel())) {
            event.reply(Strings.getString("command.wrong_channel")).setEphemeral(true).queue();
            return;
        }
        if(command.adminOnly() && (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR))) {
            event.reply(Strings.getString("command.no_permission")).setEphemeral(true).queue();
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

    /**
     * @return a {@link List<Command>} of all commands in the given guild or an empty list if the guild has no commands
     * */
    public List<Command> getCommands(Long guildId) {
        return new ArrayList<>(commands.getOrDefault(guildId, new HashMap<>()).values());
    }

    /**
     * Add global commands to the bot.
     * @param commands a {@link List<Command>} of {@link Command} to add
     * */
    public void addGlobalCommands(List<Command> commands) {
        Map<String, Command> newGlobals = new HashMap<>(this.globalCommands);
        for (Command command : commands) {
            newGlobals.put(command.getName(), command);
        }
        Main.getJDA().retrieveCommands().queue(_ -> Main.getJDA().updateCommands().addCommands(newGlobals.values().stream().map(this::mapToCommandData).toList()).queue());
        this.globalCommands.clear();
        this.globalCommands.putAll(newGlobals);
    }

    /**
     * Refreshes the commands in the given guild.
     * @param guild the guild to refresh the commands in
     * */
    public void refreshCommands(Guild guild) {
        guild.updateCommands().addCommands(commands.getOrDefault(guild.getIdLong(), new HashMap<>()).values().stream().map(this::mapToCommandData).toList()).queue();
    }

}