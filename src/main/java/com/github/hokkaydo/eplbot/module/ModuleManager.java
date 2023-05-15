package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void addModule(Module module) {
        if(getModuleByName(module.getName(), module.getGuildId(), module.getClass()).isPresent()) return;
        modules.add(module);
        Main.getCommandManager().addCommands(module.getGuildId(), module.getCommands());
    }

    public List<Module> getModules(Long guildId) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId)).toList();
    }

    public <T extends Module> List<T> getModule(@NotNull Class<T> clazz) {
        return modules.stream().filter(clazz::isInstance).map(clazz::cast).toList();
    }


    public <T extends Module> Optional<T> getModuleByName(String name, Long guildId, @NotNull Class<T> clazz) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId) && m.getName().equals(name)).filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    public boolean isModuleEnabled(Long guildId, String name) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId) && m.getName().equals(name)).findFirst().map(Module::isEnabled).orElse(false);
    }

    public void disableModule(String name, Long guildId) {
        getModuleByName(name, guildId, Module.class).ifPresent(Module::disable);
        Config.disableGuildModule(guildId, name);
    }

    public void enableModule(String name, Long guildId) {
        getModuleByName(name, guildId, Module.class).ifPresent(Module::enable);
        System.out.println("modules " + modules);
        System.out.println("enabling " + getModuleByName(name, guildId, Module.class));
        System.out.println("enabled " + name);
        Config.enableGuildModule(guildId, name);
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
