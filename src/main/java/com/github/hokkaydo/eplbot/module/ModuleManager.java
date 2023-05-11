package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Config;
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

    public List<GuildModule> getGuildModules(Long guildId) {
        return guildModules.stream().filter(m -> m.getGuildId().equals(guildId)).toList();
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
    public boolean isGlobalModuleEnabled(String name) {
        return globalModules.stream().filter(m -> m.getName().equals(name)).findFirst().map(Module::isEnabled).orElse(false);
    }

    public boolean isGuildModuleEnabled(Long guildId, String name) {
        return guildModules.stream().filter(m -> m.getGuildId().equals(guildId) && m.getName().equals(name)).findFirst().map(Module::isEnabled).orElse(false);
    }

    public boolean isGlobalModule(String name) {
        return globalModules.stream().anyMatch(m -> m.getName().equals(name));
    }

    public void disableGlobalModule(String name) {
        getGlobalModuleByName(name, GlobalModule.class).ifPresent(this::disableModule);
        Config.disableGlobalModule(name);
    }

    public void disableGuildModule(String name, Long guildId) {
        getGuildModuleByName(name, guildId, GuildModule.class).ifPresent(this::disableModule);
        Config.disableGuildModule(guildId, name);
    }

    public void enableGlobalModule(String name) {
        getGlobalModuleByName(name, GlobalModule.class).ifPresent(this::enableModule);
        Config.enableGlobalModule(name);
    }

    public void enableGuildModule(String name, Long guildId) {
        getGuildModuleByName(name, guildId, GuildModule.class).ifPresent(this::enableModule);
        Config.enableGuildModule(guildId, name);
    }

    public void enableGlobalModules(List<String> modules) {
        modules.forEach(this::enableGlobalModule);
    }

    public void enableGuildModules(Long guildId, List<String> modules) {
        guildModules.stream()
                .filter(module -> module.getGuildId().equals(guildId))
                .filter(module -> modules.contains(module.getName()))
                .forEach(Module::enable);
    }

    public List<String> getGlobalModuleNames() {
        return globalModules.stream().map(Module::getName).toList();
    }

    public List<String> getGuildModuleNames() {
        return guildModules.stream().map(Module::getName).distinct().toList();
    }

}
