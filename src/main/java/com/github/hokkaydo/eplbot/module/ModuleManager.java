package com.github.hokkaydo.eplbot.module;

import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleManager {

    private final List<GuildModule> guildModules = new ArrayList<>();
    private final List<GlobalModule> globalModules = new ArrayList<>();

    public void addGlobalModule(GlobalModule module) {
        if(getGlobalModuleByName(module.getName(), module.getClass()).isPresent()) return;
        globalModules.add(module);
    }

    public void addGuildModule(GuildModule module) {
        if(getGuildModuleByName(module.getName(), module.getGuildId(), module.getClass()).isPresent()) return;
        guildModules.add(module);
    }

    public List<GuildModule> getGuildModules(Guild guild) {
        return guildModules.stream().filter(m -> m.getGuildId().equals(guild.getIdLong())).toList();
    }

    public <T extends GuildModule> List<T> getGuildModule(@NotNull Class<T> clazz) {
        return guildModules.stream().filter(clazz::isInstance).map(clazz::cast).toList();
    }

    public <T extends GlobalModule> Optional<T> getGlobalModuleByName(String name, @NotNull Class<T> clazz) {
        return globalModules.stream().filter(m -> m.getName().equals(name)).filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    public <T extends GuildModule> Optional<T> getGuildModuleByName(String name, Long guildId, @NotNull Class<T> clazz) {
        return guildModules.stream().filter(m -> m.getGuildId().equals(guildId)).filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    public void disableModule(@NotNull Module module) {
        module.disable();
    }

    public void enableModule(@NotNull Module module) {
        module.enable();
    }

    public void disableGlobalModule(String name) {
        getGlobalModuleByName(name, GlobalModule.class).ifPresent(this::disableModule);
    }

    public void disableGuildModule(String name, Long guildId) {
        getGuildModuleByName(name, guildId, GuildModule.class).ifPresent(this::disableModule);
    }

    public void enableGlobalModule(String name) {
        getGlobalModuleByName(name, GlobalModule.class).ifPresent(this::enableModule);
    }

    public void enableGuildModule(String name, Long guildId) {
        getGuildModuleByName(name, guildId, GuildModule.class).ifPresent(this::enableModule);
    }

}
