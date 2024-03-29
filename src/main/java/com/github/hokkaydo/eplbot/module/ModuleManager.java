package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void addModules(List<Module> modules) {
        modules.stream()
                .filter(module -> getModuleByName(module.getName(), module.getGuildId(), module.getClass()).isEmpty())
                .forEach(this.modules::add);
    }

    public List<Module> getModules(Long guildId) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId)).toList();
    }


    public <T extends Module> Optional<T> getModuleByName(String name, Long guildId, @NotNull Class<T> clazz) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId) && m.getName().equals(name)).filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    public void disableModule(String name, Long guildId) {
        getModuleByName(name, guildId, Module.class).ifPresent(Module::disable);
        Config.disableModule(guildId, name);
    }

    public void enableModule(String name, Long guildId) {
        getModuleByName(name, guildId, Module.class).ifPresent(Module::enable);
        Config.enableModule(guildId, name);
    }

    public void enableModules(Long guildId, List<String> modules) {
        this.modules.stream()
                .filter(module -> module.getGuildId().equals(guildId))
                .filter(module -> modules.contains(module.getName()))
                .forEach(Module::enable);
    }

    public List<String> getModuleNames() {
        return modules.stream().map(Module::getName).distinct().toList();
    }

}
