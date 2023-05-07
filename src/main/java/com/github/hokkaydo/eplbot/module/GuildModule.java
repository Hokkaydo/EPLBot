package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class GuildModule extends Module {

    private final List<Long> jdaCommandIds = new ArrayList<>();

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

    @Override
    public void enable() {
        super.enable();
        jdaCommandIds.forEach(id -> Main.getCommandManager().removeCommand(id));
        jdaCommandIds.addAll(Main.getCommandManager().addGuildCommand(getGuildId(), getCommands()));
    }

    @Override
    public void disable() {
        super.disable();
        jdaCommandIds.forEach(id -> Main.getCommandManager().removeCommand(id));
    }
}
