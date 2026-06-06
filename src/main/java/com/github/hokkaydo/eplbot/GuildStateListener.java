package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildStateListener extends ListenerAdapter {

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Main.registerModules(event.getGuild().getIdLong());
        Main.getCommandManager().refreshCommands(event.getGuild());
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Config.removeGuild(event.getGuild().getIdLong());
    }

}
