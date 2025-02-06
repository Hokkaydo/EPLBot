package com.github.hokkaydo.eplbot;

import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildStateListener extends ListenerAdapter {

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Main.registerModules(event.getGuild().getIdLong());
        Main.getCommandManager().refreshCommands(event.getGuild());
    }

}
