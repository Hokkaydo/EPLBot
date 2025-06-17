package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class registers the state of modules for each guild.
 * Each module instance is specific to one guild.
 * */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    /**
     * Add a list of modules to the manager.
     * If a module with the same name and guildId already exists, it will not be added.
     * @param modules the list of {@link Module} to add
     * */
    public void addModules(List<Module> modules) {
        modules.stream()
                .filter(module -> getModuleByName(module.getName(), module.getGuildId(), module.getClass()).isEmpty())
                .forEach(this.modules::add);
    }

    /**
     * Get a module by its name and guildId.
     * @param name the name of the module
     * @param guildId the id of the guild
     * @param clazz the class of the module
     * @param <T> the type of the module
     * @return an {@link Optional} containing the module if it exists, empty otherwise
     * */
    public <T extends Module> Optional<T> getModuleByName(String name, Long guildId, @NotNull Class<T> clazz) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId) && m.getName().equals(name)).filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    /**
     * Get the list of modules for a specific guild.
     * @param guildId the id of the guild
     * @return the list of {@link Module} for the guild
     * */
    public List<Module> getModules(Long guildId) {
        return modules.stream().filter(m -> m.getGuildId().equals(guildId)).toList();
    }

    /**
     * Disable a module by its name and guildId.
     * @param name the name of the module
     * @param guildId the id of the guild
     * @return true if the module was disabled, false if it was already disabled or not found
     * */
    public boolean disableModule(String name, Long guildId) {
        Optional<Module> module = getModuleByName(name, guildId, Module.class);
        if (module.isEmpty()) return false;
        if (!module.get().isEnabled()) return false;
        getModuleByName(name, guildId, Module.class).ifPresent(Module::disable);
        Config.disableModule(guildId, name);
        Optional.ofNullable(Main.getJDA().getGuildById(guildId)).ifPresent(g -> {
            String log = "[%s] Module '%s' disabled".formatted(g.getName(), name);
            Main.LOGGER.info(log);
        });
        return true;
    }

    /**
     * Enable a module by its name and guildId.
     * @param name the name of the module
     * @param guildId the id of the guild
     * @return true if the module was enabled, false if it was already enabled or not found
     * */
    public boolean enableModule(String name, Long guildId) {
        Optional<Module> module = getModuleByName(name, guildId, Module.class);
        if(module.isEmpty()) return false;
        if(module.get().isEnabled()) return false;
        module.ifPresent(Module::enable);
        Config.enableModule(guildId, name);
        Optional.ofNullable(Main.getJDA().getGuildById(guildId)).ifPresent(g -> {
            String log = "[%s] Module '%s' enabled".formatted(g.getName(), name);
            Main.LOGGER.info(log);
        });
        return true;
    }

    /**
     * Enable a list of modules by their names for a guild specified by the guildId.
     * @param guildId the id of the guild
     * @param modules the list of {@link Module} names to disable
     * */
    public void enableModules(Long guildId, List<String> modules) {
        this.modules.stream()
                .filter(module -> module.getGuildId().equals(guildId))
                .filter(module -> modules.contains(module.getName()))
                .forEach(Module::enable);
    }

    /**
     * Get the list of all registered distinct module names.
     * @return the list of registered (enabled or not) {@link Module} names
     * */
    public List<String> getModuleNames() {
        return modules.stream().map(Module::getName).distinct().toList();
    }

}
