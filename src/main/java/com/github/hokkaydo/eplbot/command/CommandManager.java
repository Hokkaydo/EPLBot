package com.github.hokkaydo.eplbot.command;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandManager extends ListenerAdapter {

    private final Map<Long, Map<Class<? extends Command>, Boolean>> commandStatus = new HashMap<>();
    private final Map<Long, Map<String, Command>> commands = new HashMap<>();
    public void disableCommands(Long guildId, List<? extends Class<? extends Command>> commands) {
        if(!this.commandStatus.containsKey(guildId)) return;
        Map<Class<? extends Command>, Boolean> commandStatus = this.commandStatus.get(guildId);
        for (Class<? extends Command> command : commands) {
            commandStatus.put(command, false);
        }
        this.commandStatus.put(guildId, commandStatus);
    }

    public void enableCommands(Long guildId, List<? extends Class<? extends Command>> commands) {
        Map<Class<? extends Command>, Boolean> commandStatus = this.commandStatus.getOrDefault(guildId, new HashMap<>());
        for (Class<? extends Command> command : commands) {
            commandStatus.put(command, true);
        }
        this.commandStatus.put(guildId, commandStatus);
    }

    public void addCommands(Long guildId, List<Command> commands) {
        Map<String, Command> guildCommands = this.commands.getOrDefault(guildId, new HashMap<>());
        for (Command command : commands) {
            guildCommands.put(command.getName(), command);
        }
        Optional.ofNullable(Main.getJDA().getGuildById(guildId))
                .ifPresent(guild -> guild.updateCommands().addCommands(guildCommands.values()
                                                                               .stream()
                                                                               .map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription().get())
                                                                                                   .addOptions(cmd.getOptions())
                                                                                                   .setDefaultPermissions(cmd.adminOnly() ? DefaultMemberPermissions.DISABLED : DefaultMemberPermissions.ENABLED))
                                                                               .toList())
                                            .queue()
                );
        this.commands.put(guildId, guildCommands);
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(!event.isGuildCommand() || event.getGuild() == null) return;
        if(!commandStatus.containsKey(event.getGuild().getIdLong())) return;
        Command command = commands.get(event.getGuild().getIdLong()).get(event.getFullCommandName());
        if(command == null) return;

        if(!commandStatus.get(event.getGuild().getIdLong()).getOrDefault(command.getClass(), false)) {
            event.reply(Strings.getString("COMMAND_DISABLED")).setEphemeral(true).queue();
            return;
        }

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