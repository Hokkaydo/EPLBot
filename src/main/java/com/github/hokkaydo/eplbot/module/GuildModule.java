package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.Guild;

public abstract class GuildModule implements Module {

    private final Long guildId;

    public GuildModule(Guild guild) {
        this.guildId = guild.getIdLong();
    }

    public Long getGuildId() {
        return guildId;
    }

    public Guild getGuild() {
        return Main.getJDA().getGuildById(guildId);
    }

}
