package com.github.hokkaydo.eplbot.module.autopin;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.module.GuildModule;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

public class AutoPinModule extends GuildModule {

    private final AutoPinListener listener;

    public AutoPinModule(@NotNull Guild guild) {
        super(guild);
        listener = new AutoPinListener(guild.getIdLong());
    }

    @Override
    public void enable() {
        super.enable();
        Main.getJDA().addEventListener(listener);
    }

    @Override
    public void disable() {
        super.disable();
        Main.getJDA().removeEventListener(listener);
    }

    @Override
    public String getName() {
        return "autopin";
    }

}
