package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

public abstract class GuildModule extends Module {

    private final Long guildId;

    public GuildModule(@NotNull Guild guild) {
        this.guildId = guild.getIdLong();
    }

    public Long getGuildId() {
        return guildId;
    }

    public Guild getGuild() {
        return Main.getJDA().getGuildById(guildId);
    }

}
