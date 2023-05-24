package com.github.hokkaydo.eplbot;

import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildStateListener extends ListenerAdapter {

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Main.registerModules();
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Main.eplModuleRegisteredGuilds.remove(event.getGuild().getIdLong());
        Main.globalModuleRegisteredGuilds.remove(event.getGuild().getIdLong());
    }

}
