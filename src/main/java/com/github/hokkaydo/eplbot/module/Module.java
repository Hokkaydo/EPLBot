package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a module that can be enabled or disabled.
 * The mains methods to implement are:
 * <ul>
 * <li> {@link #getName()}: the name of the module</li>
 * <li> {@link #getCommands()}: the list of commands of the module</li>
 * <li> {@link #getListeners()}: the list of listeners of the module</li>
 * </ul>
 * Optionally,
 * you can override the {@link #enable()} and {@link #disable()} methods
 * to add custom behavior when the module is enabled or disabled.
 * <p>The {@link #logger} can be used through {@link #getLogger()} to log events related to current module</p>
 * */
public abstract class Module {

    protected boolean enabled = false;

    private final Long guildId;

    private final Logger logger = JDALogger.getLog(Strings.capsFirstLetter(getName()));

    public Module(@NotNull Long guildId) {
        this.guildId = guildId;
    }

    public Long getGuildId() {
        return guildId;
    }

    public abstract String getName();
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @return the list of module's commands. They will be enabled when the module is enabled and registered in the command manager
     * */
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    /**
     * @return the list of module's listeners. They will be loaded and unloaded when the module is enabled and disabled
     * */
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

    public Guild getGuild() {
        return Main.getJDA().getGuildById(guildId);
    }

    public void enable() {
        this.enabled = true;
        Main.getJDA().addEventListener(getListeners().toArray());
        Main.getCommandManager().enableCommands(getGuildId(), getCommands());
    }

    public void disable() {
        this.enabled = false;
        Main.getJDA().removeEventListener(getListeners().toArray());
        Main.getCommandManager().disableCommands(getGuildId(), getCommands());
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return "%s;%s".formatted(getName(), getGuild().getName());
    }

}
